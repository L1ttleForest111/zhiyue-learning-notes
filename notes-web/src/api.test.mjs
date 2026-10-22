import assert from 'node:assert/strict';
import { after, before, beforeEach, describe, it } from 'node:test';
import axios from 'axios';
import { createServer } from 'vite';

const originalAdapter = axios.defaults.adapter;
let server;
let api;
let handler;
let lastConfig;

function response(config, data, status = 200) {
  return { config, data, status, statusText: String(status), headers: {} };
}

function rejectHttp(config, data, status) {
  throw new axios.AxiosError('HTTP error', 'ERR_BAD_RESPONSE', config, undefined, response(config, data, status));
}

describe('Axios API 请求兼容性', { concurrency: false }, () => {
  before(async () => {
    axios.defaults.adapter = async (config) => {
      lastConfig = config;
      return handler(config);
    };
    server = await createServer({ server: { middlewareMode: true, watch: null }, appType: 'custom' });
    ({ api } = await server.ssrLoadModule('/src/api.ts'));
  });

  beforeEach(() => {
    lastConfig = undefined;
    handler = (config) => response(config, { code: 0, message: 'success', data: { id: 1 } });
  });

  after(async () => {
    axios.defaults.adapter = originalAdapter;
    await server?.close();
  });

  it('保留返回业务数据的调用方式，设置 10 秒超时', async () => {
    assert.deepEqual(await api.getNote(1), { id: 1 });
    assert.equal(lastConfig.url, '/notes/1');
    assert.equal(lastConfig.method, 'get');
    assert.equal(lastConfig.timeout, 10_000);
    assert.equal(lastConfig.headers.get('Accept'), 'application/json');
    assert.equal(lastConfig.headers.has('Content-Type'), false);
  });

  it('URLSearchParams 保留中文、特殊字符和重复查询参数', async () => {
    const params = new URLSearchParams({ keyword: 'React & Java/笔记', page: '1', pageSize: '20' });
    params.append('tagId', '2');
    params.append('tagId', '3');
    await api.listNotes(params);
    assert.equal(lastConfig.url, '/notes');
    const url = new URL(axios.getUri(lastConfig));
    assert.equal(url.searchParams.get('keyword'), 'React & Java/笔记');
    assert.deepEqual(url.searchParams.getAll('tagId'), ['2', '3']);
  });

  it('创建、更新、开关、分类和标签请求自动序列化 JSON', async () => {
    const payload = { title: '测试笔记', content: '# Markdown\n正文', categoryId: null, status: 'LEARNING', tagIds: [1], nextReviewAt: null };
    const cases = [
      [() => api.createNote(payload), '/notes', 'post', payload],
      [() => api.updateNote(1, payload), '/notes/1', 'put', payload],
      [() => api.patchNote(1, 'pin', true), '/notes/1/pin', 'patch', { value: true }],
      [() => api.patchNote(1, 'favorite', false), '/notes/1/favorite', 'patch', { value: false }],
      [() => api.createCategory('前端'), '/categories', 'post', { name: '前端' }],
      [() => api.createTag('React'), '/tags', 'post', { name: 'React' }],
    ];
    for (const [call, path, method, data] of cases) {
      await call();
      assert.equal(lastConfig.url, path);
      assert.equal(lastConfig.method, method);
      assert.deepEqual(JSON.parse(lastConfig.data), data);
      assert.match(lastConfig.headers.get('Content-Type'), /application\/json/);
    }
  });

  it('列表和统计查询保持原路径', async () => {
    for (const [call, path] of [[api.listCategories, '/categories'], [api.listTags, '/tags'], [api.dashboard, '/dashboard']]) {
      await call();
      assert.equal(lastConfig.url, path);
      assert.equal(lastConfig.method, 'get');
    }
  });

  it('删除请求接收 data=null，不要求笔记实体', async () => {
    handler = (config) => response(config, { code: 0, message: 'success', data: null });
    assert.equal(await api.deleteNote(1), null);
    assert.equal(lastConfig.method, 'delete');
    assert.equal(lastConfig.data, undefined);
  });

  it('HTTP 200 下业务错误仍然抛出后端提示', async () => {
    handler = (config) => response(config, { code: 400, message: '标题不能为空', data: null });
    await assert.rejects(api.getNote(1), /标题不能为空/);
  });

  it('HTTP 错误保留后端 JSON 提示', async () => {
    for (const status of [400, 404, 409, 500]) {
      handler = (config) => rejectHttp(config, { code: status, message: '后端错误提示', data: null }, status);
      await assert.rejects(api.getNote(1), /后端错误提示/);
    }
  });

  it('非 JSON 和错误响应格式不会误报成功', async () => {
    for (const body of ['<html>error</html>', '', null, {}, { code: 0, message: 'success' }]) {
      handler = (config) => response(config, body);
      await assert.rejects(api.getNote(1), /服务返回了无法解析的响应/);
    }
    handler = (config) => rejectHttp(config, '<html>Forbidden</html>', 403);
    await assert.rejects(api.getNote(1), /HTTP 403/);
  });

  it('网络错误返回清晰提示', async () => {
    handler = (config) => { throw new axios.AxiosError('Network error', 'ERR_NETWORK', config); };
    await assert.rejects(api.getNote(1), /网络连接失败，请确认后端服务是否已启动/);
  });

  it('超时提示确认操作结果，且不会自动重试写入', async () => {
    for (const code of ['ECONNABORTED', 'ETIMEDOUT']) {
      let calls = 0;
      handler = (config) => { calls += 1; throw new axios.AxiosError('timeout', code, config); };
      await assert.rejects(api.createTag('React'), /请求超时，请刷新确认操作结果/);
      assert.equal(calls, 1);
    }
  });

  it('取消请求单独提示，不误报网络失败', async () => {
    handler = () => { throw new axios.CanceledError(); };
    await assert.rejects(api.getNote(1), /请求已取消/);
  });
});
