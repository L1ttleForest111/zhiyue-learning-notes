import type { Category, Dashboard, Note, NotePayload, PageResult, Tag } from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

type ApiResponse<T> = { code: number; message: string; data: T };

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  });
  const body = await response.json() as ApiResponse<T>;
  if (!response.ok || body.code !== 0) throw new Error(body.message || '请求失败');
  return body.data;
}

export const api = {
  listNotes: (params: URLSearchParams) => request<PageResult<Note>>(`/notes?${params}`),
  getNote: (id: number) => request<Note>(`/notes/${id}`),
  createNote: (payload: NotePayload) => request<Note>('/notes', { method: 'POST', body: JSON.stringify(payload) }),
  updateNote: (id: number, payload: NotePayload) => request<Note>(`/notes/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteNote: (id: number) => request<void>(`/notes/${id}`, { method: 'DELETE' }),
  patchNote: (id: number, action: 'pin' | 'favorite', value: boolean) => request<Note>(`/notes/${id}/${action}`, { method: 'PATCH', body: JSON.stringify({ value }) }),
  listCategories: () => request<Category[]>('/categories'),
  createCategory: (name: string) => request<Category>('/categories', { method: 'POST', body: JSON.stringify({ name }) }),
  listTags: () => request<Tag[]>('/tags'),
  createTag: (name: string) => request<Tag>('/tags', { method: 'POST', body: JSON.stringify({ name }) }),
  dashboard: () => request<Dashboard>('/dashboard'),
};
