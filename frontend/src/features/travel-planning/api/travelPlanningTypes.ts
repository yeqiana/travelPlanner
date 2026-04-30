export interface ApiResult<T> {
  code: number;
  message: string;
  data: T | null;
}

export interface TravelPlanRequest {
  message: string;
  sessionId?: string;
}

export interface TravelPlanResponse {
  planId: string | null;
  sessionId: string | null;
  needClarification: boolean;
  clarificationQuestions: string[];
  structuredClarificationQuestions?: ClarificationQuestion[];
  recommendedPlan: BackendTravelPlan | null;
  reminders?: unknown[];
  risks?: string[];
  createdAt?: string;
}

export interface ClarificationQuestion {
  field?: string;
  question: string;
  example?: string;
  required?: boolean;
}

export interface BackendTravelPlan {
  title: string;
  summary: string;
  route?: string[];
  dailyPlans: BackendDailyPlan[];
  transportSuggestions?: string[];
  hotelSuggestions?: string[];
  budgetEstimate?: Record<string, unknown>;
  risks?: string[];
  todoList?: string[];
}

export interface BackendDailyPlan {
  day: number;
  city: string;
  morning?: string;
  afternoon?: string;
  evening?: string;
  fatigueLevel?: string;
  notes?: string[];
}

export interface GenerateItineraryOptions {
  sessionId?: string;
}
