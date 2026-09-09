import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  App as AntdApp,
  Button,
  Card,
  Col,
  ConfigProvider,
  Divider,
  Empty,
  Input,
  Layout,
  List,
  Menu,
  Popconfirm,
  Row,
  Select,
  Space,
  Spin,
  Statistic,
  Tag as AntTag,
  Tooltip,
  Typography,
} from 'antd';
import {
  BookOutlined,
  DashboardOutlined,
  DeleteOutlined,
  FileTextOutlined,
  FolderAddOutlined,
  FolderOutlined,
  PlusOutlined,
  PushpinFilled,
  PushpinOutlined,
  SearchOutlined,
  StarFilled,
  StarOutlined,
  TagsOutlined,
} from '@ant-design/icons';
import { api } from './api';
import type { Category, Dashboard, Note, NotePayload, NoteStatus, Tag } from './types';

const { Sider, Content } = Layout;
const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;
const EMPTY_DRAFT: NotePayload = { title: '', content: '', categoryId: null, status: 'LEARNING', tagIds: [], nextReviewAt: null };
const STATUS_LABEL: Record<NoteStatus, string> = { TODO: '待学习', LEARNING: '学习中', MASTERED: '已掌握', REVIEW: '待复习' };
const STATUS_COLOR: Record<NoteStatus, string> = { TODO: 'default', LEARNING: 'processing', MASTERED: 'success', REVIEW: 'warning' };

function toDateInput(value: string | null) {
  return value ? value.slice(0, 16).replace(' ', 'T') : '';
}

function markdownPreview(content: string) {
  if (!content.trim()) return <Text type="secondary">在左侧写下你的学习收获…</Text>;
  return content.split('\n').map((line, index) => {
    if (line.startsWith('### ')) return <Title level={5} key={index}>{line.slice(4)}</Title>;
    if (line.startsWith('## ')) return <Title level={4} key={index}>{line.slice(3)}</Title>;
    if (line.startsWith('# ')) return <Title level={3} key={index}>{line.slice(2)}</Title>;
    if (line.startsWith('- ')) return <li key={index}>{line.slice(2)}</li>;
    if (line.startsWith('> ')) return <blockquote key={index}>{line.slice(2)}</blockquote>;
    return line ? <Paragraph key={index}>{line}</Paragraph> : <br key={index} />;
  });
}

export default function App() {
  return <ConfigProvider theme={{ token: { colorPrimary: '#536d2d', borderRadius: 8, fontFamily: 'Inter, PingFang SC, Microsoft YaHei, sans-serif' } }}><AntdApp><NotesWorkspace /></AntdApp></ConfigProvider>;
}

function NotesWorkspace() {
  const { message } = AntdApp.useApp();
  const [notes, setNotes] = useState<Note[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [selected, setSelected] = useState<Note | null>(null);
  const [draft, setDraft] = useState<NotePayload>(EMPTY_DRAFT);
  const [keyword, setKeyword] = useState('');
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [view, setView] = useState<'notes' | 'dashboard'>('notes');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const loadNotes = useCallback(async () => {
    const params = new URLSearchParams({ page: '1', pageSize: '50' });
    if (keyword.trim()) params.set('keyword', keyword.trim());
    if (categoryId !== null) params.set('categoryId', String(categoryId));
    const result = await api.listNotes(params);
    setNotes(result.records);
  }, [keyword, categoryId]);

  const loadAll = useCallback(async () => {
    setLoading(true);
    try {
      const [categoryData, tagData, dashboardData] = await Promise.all([api.listCategories(), api.listTags(), api.dashboard()]);
      setCategories(categoryData);
      setTags(tagData);
      setDashboard(dashboardData);
      await loadNotes();
    } catch (cause) {
      message.error(cause instanceof Error ? cause.message : '无法连接本地后端');
    } finally {
      setLoading(false);
    }
  }, [loadNotes, message]);

  useEffect(() => { void loadAll(); }, [loadAll]);

  const selectNote = async (id: number) => {
    try {
      const note = await api.getNote(id);
      setSelected(note);
      setDraft({ title: note.title, content: note.content, categoryId: note.categoryId, status: note.status, tagIds: note.tags.map((tag) => tag.id), nextReviewAt: note.nextReviewAt });
      setView('notes');
    } catch (cause) {
      message.error(cause instanceof Error ? cause.message : '读取笔记失败');
    }
  };

  const createNote = () => {
    setSelected(null);
    setDraft(EMPTY_DRAFT);
    setView('notes');
  };

  const save = async () => {
    if (!draft.title.trim()) { message.warning('请先填写笔记标题'); return; }
    setSaving(true);
    try {
      const normalized = { ...draft, title: draft.title.trim(), nextReviewAt: draft.nextReviewAt || null };
      const note = selected ? await api.updateNote(selected.id, normalized) : await api.createNote(normalized);
      setSelected(note);
      setDraft({ title: note.title, content: note.content, categoryId: note.categoryId, status: note.status, tagIds: note.tags.map((tag) => tag.id), nextReviewAt: note.nextReviewAt });
      message.success('已保存到本地数据库');
      await loadAll();
    } catch (cause) {
      message.error(cause instanceof Error ? cause.message : '保存失败');
    } finally {
      setSaving(false);
    }
  };

  const remove = async () => {
    if (!selected) return;
    try {
      await api.deleteNote(selected.id);
      createNote();
      await loadAll();
      message.success('笔记已删除');
    } catch (cause) {
      message.error(cause instanceof Error ? cause.message : '删除失败');
    }
  };

  const toggle = async (action: 'pin' | 'favorite') => {
    if (!selected) return;
    try {
      const value = action === 'pin' ? !selected.pinned : !selected.favorite;
      const note = await api.patchNote(selected.id, action, value);
      setSelected(note);
      await loadAll();
    } catch (cause) {
      message.error(cause instanceof Error ? cause.message : '更新失败');
    }
  };

  const addCatalog = async (kind: 'category' | 'tag') => {
    const name = window.prompt(`新建${kind === 'category' ? '分类' : '标签'}名称`);
    if (!name?.trim()) return;
    try {
      if (kind === 'category') {
        const category = await api.createCategory(name.trim());
        setCategories((current) => [...current, category]);
      } else {
        const tag = await api.createTag(name.trim());
        setTags((current) => [...current, tag]);
      }
      message.success(`${kind === 'category' ? '分类' : '标签'}已创建`);
    } catch (cause) {
      message.error(cause instanceof Error ? cause.message : '创建失败');
    }
  };

  const updateDraft = <K extends keyof NotePayload>(key: K, value: NotePayload[K]) => setDraft((current) => ({ ...current, [key]: value }));
  const menuItems = [
    { key: 'dashboard', icon: <DashboardOutlined />, label: '学习概览' },
    { key: 'notes', icon: <BookOutlined />, label: '全部笔记' },
  ];

  return <Layout className="app-layout">
    <Sider width={244} theme="dark" className="app-sider">
      <div className="brand"><BookOutlined /><span>知页</span></div>
      <Menu theme="dark" mode="inline" selectedKeys={[view]} items={menuItems} onClick={({ key }) => setView(key as 'notes' | 'dashboard')} />
      <section className="sider-section"><div className="sider-title"><span>分类</span><Button type="text" size="small" icon={<PlusOutlined />} onClick={() => void addCatalog('category')} /></div>
        <Menu theme="dark" mode="inline" selectable selectedKeys={[categoryId === null ? 'all' : String(categoryId)]} items={[{ key: 'all', icon: <FolderOutlined />, label: '全部内容' }, ...categories.map((category) => ({ key: String(category.id), icon: <FolderOutlined />, label: category.name }))]} onClick={({ key }) => { setCategoryId(key === 'all' ? null : Number(key)); setView('notes'); }} />
      </section>
      <section className="sider-section"><div className="sider-title"><span>标签</span><Button type="text" size="small" icon={<PlusOutlined />} onClick={() => void addCatalog('tag')} /></div>
        <div className="sider-tags">{tags.length ? tags.map((tag) => <AntTag key={tag.id}>#{tag.name}</AntTag>) : <Text type="secondary">暂未创建标签</Text>}</div>
      </section>
      <Text className="local-mode">本地模式 · 无需登录</Text>
    </Sider>

    <Layout>
      <Content className="page-content">
        {view === 'dashboard'
          ? <DashboardPanel dashboard={dashboard} loading={loading} onOpen={selectNote} onCreate={createNote} />
          : <NotesPanel notes={notes} categories={categories} tags={tags} selected={selected} draft={draft} keyword={keyword} loading={loading} saving={saving} onKeyword={setKeyword} onSearch={() => void loadAll()} onSelect={selectNote} onCreate={createNote} onChange={updateDraft} onSave={() => void save()} onDelete={() => void remove()} onToggle={toggle} />}
      </Content>
    </Layout>
  </Layout>;
}

function DashboardPanel({ dashboard, loading, onOpen, onCreate }: { dashboard: Dashboard | null; loading: boolean; onOpen: (id: number) => void; onCreate: () => void }) {
  const cards = dashboard ? [
    ['总笔记', dashboard.totalNotes, <FileTextOutlined key="notes" />],
    ['学习中', dashboard.learningNotes, <BookOutlined key="learning" />],
    ['已掌握', dashboard.masteredNotes, <StarOutlined key="mastered" />],
    ['待复习', dashboard.dueReviews, <PushpinOutlined key="review" />],
  ] : [];
  return <div className="content-container"><PageHeader title="今天，继续构建你的知识库" subtitle="学习概览" action={<Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>新建笔记</Button>} />
    <Spin spinning={loading}><Row gutter={[16, 16]}>{cards.map(([label, value, icon]) => <Col xs={24} sm={12} lg={6} key={String(label)}><Card><Statistic title={label} value={value as number} prefix={icon} /></Card></Col>)}</Row>
      <Card className="recent-card" title="最近编辑" extra={<Text type="secondary">保留你的思考轨迹</Text>}>
        {dashboard?.recentNotes.length ? <List grid={{ gutter: 16, xs: 1, sm: 2, lg: 3 }} dataSource={dashboard.recentNotes} renderItem={(note) => <List.Item><Card size="small" hoverable onClick={() => onOpen(note.id)}><Space direction="vertical" size={6} className="full-width"><AntTag color={STATUS_COLOR[note.status]}>{STATUS_LABEL[note.status]}</AntTag><Text strong ellipsis>{note.title}</Text><Paragraph type="secondary" ellipsis={{ rows: 2 }}>{plainText(note.content)}</Paragraph><Text type="secondary" className="date-text">{note.updatedAt?.slice(0, 10)}</Text></Space></Card></List.Item>} /> : <Empty description="还没有记录，开始第一篇学习笔记吧" />}
      </Card>
    </Spin>
  </div>;
}

function NotesPanel({ notes, categories, tags, selected, draft, keyword, loading, saving, onKeyword, onSearch, onSelect, onCreate, onChange, onSave, onDelete, onToggle }: {
  notes: Note[]; categories: Category[]; tags: Tag[]; selected: Note | null; draft: NotePayload; keyword: string; loading: boolean; saving: boolean;
  onKeyword: (value: string) => void; onSearch: () => void; onSelect: (id: number) => void; onCreate: () => void; onChange: <K extends keyof NotePayload>(key: K, value: NotePayload[K]) => void; onSave: () => void; onDelete: () => void; onToggle: (action: 'pin' | 'favorite') => void;
}) {
  return <div className="content-container notes-content"><PageHeader title="学习笔记" subtitle="我的知识库" action={<Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>新建笔记</Button>} />
    <div className="notes-workspace">
      <Card className="note-sidebar" size="small" title="笔记列表">
        <Input.Search allowClear placeholder="搜索标题或内容" prefix={<SearchOutlined />} value={keyword} onChange={(event) => onKeyword(event.target.value)} onSearch={onSearch} enterButton="搜索" />
        <div className="list-summary"><Text type="secondary">{notes.length} 篇笔记</Text><Text type="secondary">最近更新</Text></div>
        <Spin spinning={loading}>{notes.length ? <List className="note-list" dataSource={notes} renderItem={(note) => <List.Item className={selected?.id === note.id ? 'selected-note' : ''} onClick={() => onSelect(note.id)}><List.Item.Meta title={<Space size={4}>{note.pinned && <PushpinFilled className="pinned-icon" />}<Text strong ellipsis>{note.title}</Text></Space>} description={<Space direction="vertical" size={2} className="full-width"><Text type="secondary" ellipsis>{plainText(note.content) || '暂无正文'}</Text><Space><AntTag color={STATUS_COLOR[note.status]}>{STATUS_LABEL[note.status]}</AntTag><Text type="secondary" className="date-text">{note.updatedAt?.slice(0, 10)}</Text></Space></Space>} /></List.Item>} /> : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="还没有笔记"><Button type="link" onClick={onCreate}>写下第一篇</Button></Empty>}</Spin>
      </Card>
      <Editor draft={draft} categories={categories} tags={tags} selected={selected} saving={saving} onChange={onChange} onSave={onSave} onDelete={onDelete} onToggle={onToggle} />
    </div>
  </div>;
}

function PageHeader({ title, subtitle, action }: { title: string; subtitle: string; action: React.ReactNode }) {
  return <header className="page-header"><div><Text type="secondary">{subtitle}</Text><Title level={2}>{title}</Title></div>{action}</header>;
}

function Editor({ draft, categories, tags, selected, saving, onChange, onSave, onDelete, onToggle }: { draft: NotePayload; categories: Category[]; tags: Tag[]; selected: Note | null; saving: boolean; onChange: <K extends keyof NotePayload>(key: K, value: NotePayload[K]) => void; onSave: () => void; onDelete: () => void; onToggle: (action: 'pin' | 'favorite') => void }) {
  const toggleTag = (tagId: number) => onChange('tagIds', draft.tagIds.includes(tagId) ? draft.tagIds.filter((id) => id !== tagId) : [...draft.tagIds, tagId]);
  return <Card className="editor-card" title={selected ? '编辑笔记' : '新建笔记'} extra={<Space>{selected && <><Tooltip title="置顶"><Button type="text" icon={selected.pinned ? <PushpinFilled /> : <PushpinOutlined />} onClick={() => onToggle('pin')} /></Tooltip><Tooltip title="收藏"><Button type="text" icon={selected.favorite ? <StarFilled /> : <StarOutlined />} onClick={() => onToggle('favorite')} /></Tooltip></>}<Popconfirm title="确认删除这篇笔记？" okText="删除" cancelText="取消" onConfirm={onDelete} disabled={!selected}><Button danger type="text" icon={<DeleteOutlined />} disabled={!selected}>删除</Button></Popconfirm><Button type="primary" loading={saving} onClick={onSave}>保存笔记</Button></Space>}>
    <Space direction="vertical" size="middle" className="full-width">
      <Input size="large" value={draft.title} maxLength={200} placeholder="给这篇笔记起个标题" onChange={(event) => onChange('title', event.target.value)} />
      <Row gutter={[12, 12]}><Col xs={24} md={8}><Select className="full-width" value={draft.categoryId ?? undefined} placeholder="选择分类" allowClear options={categories.map((category) => ({ value: category.id, label: category.name }))} onChange={(value) => onChange('categoryId', value ?? null)} /></Col><Col xs={24} md={8}><Select className="full-width" value={draft.status} options={Object.entries(STATUS_LABEL).map(([value, label]) => ({ value, label }))} onChange={(value) => onChange('status', value as NoteStatus)} /></Col><Col xs={24} md={8}><Input type="datetime-local" value={toDateInput(draft.nextReviewAt)} onChange={(event) => onChange('nextReviewAt', event.target.value || null)} /></Col></Row>
      <div><Text type="secondary"><TagsOutlined /> 标签</Text><div className="tag-selector">{tags.length ? tags.map((tag) => <AntTag.CheckableTag key={tag.id} checked={draft.tagIds.includes(tag.id)} onChange={() => toggleTag(tag.id)}>#{tag.name}</AntTag.CheckableTag>) : <Text type="secondary">先在左侧创建标签</Text>}</div></div>
      <Divider />
      <Row gutter={[16, 16]}><Col xs={24} lg={12}><Text strong>Markdown</Text><TextArea className="markdown-input" value={draft.content} placeholder={'# 今天学到了什么？\n\n- 记录核心概念\n- 写下自己的理解\n- 补充一个小例子'} onChange={(event) => onChange('content', event.target.value)} /></Col><Col xs={24} lg={12}><Text strong>预览</Text><article className="markdown-preview">{markdownPreview(draft.content)}</article></Col></Row>
    </Space>
  </Card>;
}

function plainText(content: string) {
  return content.replace(/[#>*_-]/g, '').replace(/\s+/g, ' ').trim();
}
