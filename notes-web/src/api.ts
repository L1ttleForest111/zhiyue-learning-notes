import axios from 'axios';
import type { AxiosRequestConfig } from 'axios';
import type { Category, Dashboard, Note, NotePayload, PageResult, Tag, TravelNote, TravelNoteSummary, TravelNotePayload, TravelCapabilities } from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';
const http = axios.create({
  baseURL: BASE_URL,
  timeout: 10_000,
  headers: { Accept: 'application/json' },
});

type ApiResponse<T> = { code: number; message: string; data: T };

function isApiResponse(value: unknown): value is ApiResponse<unknown> {
  return typeof value === 'object' && value !== null
    && 'code' in value && typeof value.code === 'number'
    && 'message' in value && typeof value.message === 'string'
    && 'data' in value;
}

async function request<T>(path: string, config?: AxiosRequestConfig): Promise<T> {
  try {
    const response = await http.request<unknown>({ ...config, url: path });
    const body = response.data;
    if (!isApiResponse(body)) {
      throw new Error('服务返回了无法解析的响应');
    }
    if (body.code !== 0) {
      throw new Error(body.message || '请求处理失败');
    }
    return body.data as T;
  } catch (error) {
    if (axios.isCancel(error)) {
      throw new Error('请求已取消');
    }
    if (axios.isAxiosError(error)) {
      if (error.response) {
        const body: unknown = error.response.data;
        throw new Error(isApiResponse(body) && body.message
          ? body.message
          : `请求失败（HTTP ${error.response.status}）`);
      }
      if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
        throw new Error('请求超时，请刷新确认操作结果');
      }
      throw new Error('网络连接失败，请确认后端服务是否已启动');
    }
    throw error;
  }
}

export const api = {
  listNotes: (params: URLSearchParams) => request<PageResult<Note>>('/notes', { params }),
  getNote: (id: number) => request<Note>(`/notes/${id}`),
  createNote: (payload: NotePayload) => request<Note>('/notes', { method: 'POST', data: payload }),
  updateNote: (id: number, payload: NotePayload) => request<Note>(`/notes/${id}`, { method: 'PUT', data: payload }),
  deleteNote: (id: number) => request<void>(`/notes/${id}`, { method: 'DELETE' }),
  patchNote: (id: number, action: 'pin' | 'favorite', value: boolean) => request<Note>(`/notes/${id}/${action}`, { method: 'PATCH', data: { value } }),
  listCategories: () => request<Category[]>('/categories'),
  createCategory: (name: string) => request<Category>('/categories', { method: 'POST', data: { name } }),
  listTags: () => request<Tag[]>('/tags'),
  createTag: (name: string) => request<Tag>('/tags', { method: 'POST', data: { name } }),
  dashboard: () => request<Dashboard>('/dashboard'),
  listTravelNotes: (params: URLSearchParams) => request<PageResult<TravelNoteSummary>>('/travel-notes', { params }),
  getTravelNote: (id: number) => request<TravelNote>(`/travel-notes/${id}`),
  createTravelNote: (payload: TravelNotePayload) => request<TravelNote>('/travel-notes', { method: 'POST', data: payload }),
  updateTravelNote: (id: number, payload: TravelNotePayload) => request<TravelNote>(`/travel-notes/${id}`, { method: 'PUT', data: payload }),
  deleteTravelNote: (id: number) => request<void>(`/travel-notes/${id}`, { method: 'DELETE' }),
  travelCapabilities: () => request<TravelCapabilities>('/travel-notes/capabilities'),
  getTravelMapPreview: (id: number, signal?: AbortSignal) => request<{ previewUrl: string; mapConfigured: boolean }>(`/travel-notes/${id}/map-preview`, { signal }),
  getTravelPreview: (id: number, signal?: AbortSignal) => request<{ htmlContent: string }>(`/travel-notes/${id}/preview-document`, { signal }),
  travelPreviewUrl: (id: number) => `${BASE_URL}/travel-notes/${id}/preview`,
};
