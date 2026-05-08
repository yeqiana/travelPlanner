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
  intent?: BackendTravelIntent | null;
  evidences?: BackendTravelEvidence[];
  recommendedPlan: BackendTravelPlan | null;
  score?: unknown;
  imageBrief?: BackendImageBrief | null;
  reminders?: BackendTravelReminder[];
  risks?: string[];
  dialogIntent?: string | null;
  contextualSuggestions?: string[];
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

export interface BackendTravelIntent {
  departureCity?: string | null;
  dateText?: string | null;
  days?: number | null;
  peopleCount?: number | null;
  budget?: number | null;
  destinationPreferences?: string[];
  travelStyles?: string[];
  transportPreference?: string | null;
  hotelBudgetPerNight?: number | null;
  avoidPlaces?: string[];
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

export interface BackendTravelEvidence {
  evidenceType?: string;
  city?: string | null;
  title?: string | null;
  summary?: string | null;
  keyFacts?: Record<string, unknown>;
  confidence?: number;
  sourceName?: string | null;
  sourceUrl?: string | null;
  fetchedAt?: string | null;
}

export interface BackendTravelReminder {
  title?: string;
  type?: string;
  remindRule?: string;
  description?: string;
}

export interface BackendImageBrief {
  title?: string;
  subtitle?: string;
  sections?: BackendImageBriefSection[];
  routeLine?: string[];
  dayCards?: BackendImageBriefDayCard[];
  budgetCards?: BackendImageBriefBudgetCard[];
  riskTags?: string[];
  reminderCards?: BackendImageBriefReminderCard[];
  footerNote?: string;
}

export interface BackendImageBriefSection {
  title?: string;
  content?: string;
}

export interface BackendImageBriefDayCard {
  day: number;
  title?: string;
  items?: string[];
}

export interface BackendImageBriefBudgetCard {
  name?: string;
  value?: string;
}

export interface BackendImageBriefReminderCard {
  title?: string;
  time?: string;
}

export interface GenerateItineraryOptions {
  sessionId?: string;
  onStreamEvent?: (eventName: string, data: string) => void;
}
