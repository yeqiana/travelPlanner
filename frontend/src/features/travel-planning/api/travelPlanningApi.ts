import { ChatMessage, Itinerary, TravelPreferences } from '../../../shared/types/travel';
import { generateItineraryWithBackend } from './backendTravelPlanningProvider';
import { GenerateItineraryOptions } from './travelPlanningTypes';

export function generateItinerary(
  input: TravelPreferences | string,
  history: ChatMessage[] = [],
  options: GenerateItineraryOptions = {},
): Promise<Itinerary> {
  return generateItineraryWithBackend(input, history, options);
}
