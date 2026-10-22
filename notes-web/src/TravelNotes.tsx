import { useEffect, useRef, useState } from 'react';
import { Alert, App, Button, Card, Col, Empty, Form, Input, Modal, Pagination, Popconfirm, Row, Space, Spin, Tabs, Tag, Typography, Upload } from 'antd';
import { CompassOutlined, DeleteOutlined, DownloadOutlined, PlusOutlined, SaveOutlined, UploadOutlined } from '@ant-design/icons';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeHighlight from 'rehype-highlight';
import { api } from './api';
import TravelPreview from './TravelPreview';
import type { TravelNote, TravelNotePayload, TravelNoteSummary } from './types';

const MAX_HTML_BYTES = 2 * 1024 * 1024;
const blank = (): TravelNotePayload => ({ title: '', destination: '', startDate: null, endDate: null, summary: '', journalContent: '', htmlContent: '', originalFilename: '', importKey: crypto.randomUUID() });

export default function TravelNotes() {
  const { message, modal } = App.useApp();
  const [notes, setNotes] = useState<TravelNoteSummary[]>([]);
  const [draft, setDraft] = useState<TravelNotePayload>(blank);
  const [selected, setSelected] = useState<TravelNote | null>(null);
  const [page, setPage] = useState(1);
  const [keyword, setKeyword] = useState('');
  const [search, setSearch] = useState('');
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [mapConfigured, setMapConfigured] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [revision, setRevision] = useState(0);
  const openSequence = useRef(0);
  const busy = useRef(false);
  const [form] = Form.useForm();
  const change = <K extends keyof TravelNotePayload>(key: K, value: TravelNotePayload[K]) => setDraft(current => ({ ...current, [key]: value }));
  const dirty = !!selected && JSON.stringify(toPayload(selected)) !== JSON.stringify(draft);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');
    const params = new URLSearchParams({ page: String(page), pageSize: '10' });
    if (search) params.set('keyword', search);
    api.listTravelNotes(params).then(result => {
      if (!cancelled) { setNotes(result.records); setTotal(result.total); }
    }).catch(cause => { if (!cancelled) setError(cause instanceof Error ? cause.message : '旅行列表读取失败'); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [page, search, revision]);

  useEffect(() => {
    let cancelled = false;
    api.travelCapabilities().then(info => { if (!cancelled) setMapConfigured(info.mapConfigured); }).catch(() => {});
    return () => { cancelled = true; };
  }, []);

  useEffect(() => { form.setFieldsValue(draft); }, [draft, form]);

  const discard = (next: () => void) => {
    if (dirty || (!selected && (draft.htmlContent || draft.title))) {
      modal.confirm({ title: '放弃尚未保存的旅行记录？', okText: '放弃并继续', cancelText: '继续编辑', onOk: next });
    } else next();
  };

  const reset = () => { openSequence.current++; setSelected(null); setDraft(blank()); form.resetFields(); };
  const openNote = async (id: number) => {
    const sequence = ++openSequence.current;
    try {
      const note = await api.getTravelNote(id);
      if (sequence === openSequence.current) { setSelected(note); setDraft(toPayload(note)); }
    } catch (cause) { message.error(cause instanceof Error ? cause.message : '读取失败'); }
  };

  const readFile = async (file: File) => {
    if (busy.current || saving) return false;
    if (!/\.html?$/i.test(file.name)) { message.warning('请选择 .html 或 .htm 文件'); return false; }
    if (file.size > MAX_HTML_BYTES) { message.warning('HTML 文件不能超过 2 MiB'); return false; }
    busy.current = true;
    try {
      const html = new TextDecoder('utf-8', { fatal: true }).decode(await file.arrayBuffer());
      if (!html.trim()) throw new Error('文件内容为空');
      const template = document.createElement('template');
      template.innerHTML = html;
      const doc = template.content;
      const title = doc.querySelector('title')?.textContent?.trim() || file.name.replace(/\.html?$/i, '');
      setDraft(current => ({ ...current, htmlContent: html, originalFilename: file.name, title: current.title || title.slice(0, 200) }));
      if (/__WB_HTTP_PORT__|__WB_TMAP_SECRET__|_TMapSecurityConfig/.test(html)) message.info('检测到 WorkBuddy 地图代理，预览时会移除；公开 SDK 可尝试底图，正式授权需个人 Key');
      if ([...doc.querySelectorAll('[href],[src]')].some(el => /^(\.\/|\.\.\/)/.test(el.getAttribute('href') || el.getAttribute('src') || ''))) message.info('发现相对附件引用：该文件未包含附件，预览不会加载这些链接');
      message.success('HTML 已读取，点击“保存旅行”后才会存入数据库');
    } catch (cause) { message.error(cause instanceof Error ? cause.message : '文件读取失败'); }
    finally { busy.current = false; }
    return false;
  };

  const save = async () => {
    if (busy.current) return;
    if (!draft.htmlContent) { message.warning('先选择 HTML 攻略文件'); return; }
    busy.current = true; setSaving(true);
    try {
      try { await form.validateFields(); } catch { return; }
      const payload = { ...draft, title: draft.title.trim() };
      const note = selected ? await api.updateTravelNote(selected.id, payload) : await api.createTravelNote(payload);
      setSelected(note); setDraft(toPayload(note)); setRevision(value => value + 1);
      message.success('旅行攻略和心得已保存到 MySQL');
    } catch (cause) { message.error(cause instanceof Error ? cause.message : '保存失败'); }
    finally { busy.current = false; setSaving(false); }
  };

  const remove = async () => {
    if (!selected || busy.current) return;
    busy.current = true;
    try {
      await api.deleteTravelNote(selected.id); reset(); setPage(1); setRevision(value => value + 1); message.success('旅行笔记已删除');
    } catch (cause) { message.error(cause instanceof Error ? cause.message : '删除失败'); }
    finally { busy.current = false; }
  };

  const download = () => {
    if (!selected) return;
    const url = URL.createObjectURL(new Blob([selected.htmlContent], { type: 'text/html;charset=utf-8' }));
    const anchor = document.createElement('a'); anchor.href = url; anchor.download = selected.originalFilename; anchor.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    message.info('下载的是原始 HTML；在浏览器单独打开会执行其中的脚本，请仅打开可信文件');
  };

  return <div className="content-container travel-content">
    <header className="page-header"><div><Typography.Text type="secondary">把每次出发，变成一份可回看的记忆</Typography.Text><Typography.Title level={2}>旅行笔记</Typography.Title></div><Button type="primary" icon={<PlusOutlined />} disabled={saving} onClick={() => discard(reset)}>新建旅行</Button></header>
    {error && <Alert type="error" showIcon title={error} action={<Button onClick={() => setRevision(value => value + 1)}>重试</Button>} className="travel-alert" />}
    <Alert type="info" showIcon title="HTML 可以存档，地图不等于离线保存" description={mapConfigured ? '已配置地图 Key。预览按受限网络策略运行，地图仍受域名授权、配额和 SDK 兼容性影响。' : '地图 SDK 已启用，将在独立本机预览源尝试加载底图和点位。尚未配置个人 Key，授权与路线计算不保证可用；不使用 WorkBuddy 代理，相对 PDF 不会自动迁移。'} className="travel-alert" />
    <div className="travel-workspace">
      <Card title={<Space><CompassOutlined />我的旅行 <Tag>{total}</Tag></Space>} className="travel-list">
        <Input.Search value={keyword} maxLength={200} allowClear placeholder="搜索目的地或攻略标题" onChange={e => setKeyword(e.target.value)} onSearch={value => { setPage(1); setSearch(value.trim()); }} />
        <Spin spinning={loading}><div className="travel-cards">{notes.length ? notes.map(note => <Card key={note.id} size="small" hoverable className={selected?.id === note.id ? 'travel-selected' : ''} onClick={() => !saving && discard(() => { void openNote(note.id); })}>
          <Typography.Text strong>{note.title}</Typography.Text><div><Tag>{note.destination || '目的地待补充'}</Tag><Typography.Text type="secondary">{note.startDate || '日期未定'}</Typography.Text></div><Typography.Paragraph type="secondary" ellipsis={{ rows: 2 }}>{note.summary || note.originalFilename}</Typography.Paragraph>
        </Card>) : <Empty description="导入 HTML，记录第一段旅程" image={Empty.PRESENTED_IMAGE_SIMPLE} />}</div></Spin>
        <Pagination simple current={page} total={total} pageSize={10} onChange={setPage} />
      </Card>
      <Card title={selected ? '编辑旅行记录' : '新旅行'} extra={<Space><Popconfirm title="删除这份旅行记录？" okText="删除" cancelText="取消" onConfirm={remove} disabled={!selected || saving}><Button danger icon={<DeleteOutlined />} disabled={!selected || saving} /></Popconfirm><Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={() => void save()}>保存旅行</Button></Space>}>
        <Form form={form} layout="vertical" disabled={saving}>
          <Form.Item name="title" label="攻略标题" rules={[{ required: true, whitespace: true, message: '请填写标题' }]}><Input maxLength={200} onChange={e => change('title', e.target.value)} /></Form.Item>
          <Row gutter={12}><Col xs={24} md={8}><Form.Item label="目的地"><Input value={draft.destination} maxLength={100} onChange={e => change('destination', e.target.value)} placeholder="例如：阳朔" /></Form.Item></Col><Col xs={12} md={8}><Form.Item label="开始日期"><Input type="date" value={draft.startDate || ''} onChange={e => change('startDate', e.target.value || null)} /></Form.Item></Col><Col xs={12} md={8}><Form.Item label="结束日期"><Input type="date" value={draft.endDate || ''} min={draft.startDate || undefined} onChange={e => change('endDate', e.target.value || null)} /></Form.Item></Col></Row>
          <Form.Item label="摘要"><Input.TextArea value={draft.summary} rows={2} maxLength={1000} onChange={e => change('summary', e.target.value)} placeholder="路线亮点、旅行安排…" /></Form.Item>
          <Form.Item label="HTML 攻略文件"><Space wrap><Upload accept=".html,.htm" showUploadList={false} beforeUpload={readFile}><Button icon={<UploadOutlined />} disabled={saving}>{draft.htmlContent ? '替换攻略文件' : '选择 HTML 文件'}</Button></Upload><Typography.Text type="secondary">{draft.originalFilename || 'UTF-8 编码，最多 2 MiB'}</Typography.Text></Space></Form.Item>
        </Form>
        <Tabs items={[
          { key: 'guide', label: '攻略预览', children: <Space orientation="vertical" className="full-width"><Typography.Paragraph type="secondary">保存后可打开隔离预览。原脚本只在沙箱中运行，禁止访问知页页面、跳转主页面或访问本机代理。地图需要联网，底图加载成功不代表路线服务鉴权成功；正式使用需个人 Key。</Typography.Paragraph><Space><Button disabled={!selected || dirty} onClick={() => setPreviewOpen(true)}>打开已保存攻略</Button><Button icon={<DownloadOutlined />} disabled={!selected} onClick={download}>下载已保存 HTML</Button></Space>{dirty && <Typography.Text type="warning">当前有未保存修改，请先保存后预览。</Typography.Text>}</Space> },
          { key: 'journal', label: '我的心得（Markdown）', children: <Row gutter={12}><Col xs={24} lg={12}><Input.TextArea value={draft.journalContent} rows={12} maxLength={100000} placeholder="# 实际体验\n\n花费、拍照点位、避坑记录…" onChange={e => change('journalContent', e.target.value)} /></Col><Col xs={24} lg={12}><article className="markdown-preview"><ReactMarkdown remarkPlugins={[remarkGfm]} rehypePlugins={[rehypeHighlight]}>{draft.journalContent || '还没有心得，旅途回来记一记。'}</ReactMarkdown></article></Col></Row> },
        ]} />
      </Card>
    </div>
    <Modal open={previewOpen} onCancel={() => setPreviewOpen(false)} footer={null} width="95vw" title={selected?.title} destroyOnHidden>
      {previewOpen && selected && <TravelPreview noteId={selected.id} />}
    </Modal>
  </div>;
}

function toPayload(note: TravelNote): TravelNotePayload {
  return { title: note.title, destination: note.destination, startDate: note.startDate, endDate: note.endDate,
    summary: note.summary, journalContent: note.journalContent, htmlContent: note.htmlContent,
    originalFilename: note.originalFilename, importKey: note.importKey };
}
