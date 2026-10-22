export type NoteStatus = 'TODO' | 'LEARNING' | 'MASTERED' | 'REVIEW';

export interface Category { id: number; name: string; sortOrder: number; createdAt: string; }
export interface Tag { id: number; name: string; createdAt: string; }
export interface Note {
  id: number; title: string; content: string; categoryId: number | null; categoryName: string | null;
  status: NoteStatus; pinned: boolean; favorite: boolean; nextReviewAt: string | null;
  createdAt: string; updatedAt: string; tags: Tag[];
}
export interface PageResult<T> { records: T[]; total: number; page: number; pageSize: number; }
export interface Dashboard { totalNotes: number; learningNotes: number; masteredNotes: number; dueReviews: number; recentNotes: Note[]; }
export interface NotePayload { title: string; content: string; categoryId: number | null; status: NoteStatus; tagIds: number[]; nextReviewAt: string | null; }

export interface TravelNoteSummary {
  id: number; title: string; destination: string; startDate: string | null; endDate: string | null;
  summary: string; originalFilename: string; createdAt: string; updatedAt: string;
}
export interface TravelNote extends TravelNoteSummary {
  journalContent: string; htmlContent: string; importKey: string; htmlSha256: string;
}
export interface TravelNotePayload {
  title: string; destination: string; startDate: string | null; endDate: string | null;
  summary: string; journalContent: string; htmlContent: string; originalFilename: string; importKey: string;
}
export interface TravelCapabilities { mapConfigured: boolean; maxHtmlBytes: number; }
