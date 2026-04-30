export interface PriceEstimate {
  hotelMin: number;
  hotelMax: number;
  restaurantMin: number;
  restaurantMax: number;
}

export interface Activity {
  time: string;
  location: string;
  description: string;
  duration: string;
  transportationToNext?: string;
  priceEstimate?: PriceEstimate;
}

export interface DayPlan {
  dayNumber: number;
  theme: string;
  activities: Activity[];
}

export interface Itinerary {
  title: string;
  summary: string;
  days: DayPlan[];
  tips: string[];
  assistantReply?: string;
}

export interface UserPreferences {
  travelStyle: string;
  accommodationType: string;
  diningPreference: string;
  dietaryRestrictions: string;
}

export interface TravelPreferences {
  destinations: string;
  days: number;
  vibe: string;
  companions: string;
  additionalNotes?: string;
}

export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  text: string;
  itinerary?: Itinerary;
  isLoading?: boolean;
}

export interface ChatSession {
  id: string;
  title: string;
  updatedAt: number;
  history: ChatMessage[];
}

