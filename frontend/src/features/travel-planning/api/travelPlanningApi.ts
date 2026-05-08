import { ChatMessage, Itinerary, TravelPreferences } from '../../../shared/types/travel';
import { generateItineraryWithBackend } from './backendTravelPlanningProvider';
import { generateItineraryWithStream } from './streamTravelPlanningProvider';
import { GenerateItineraryOptions } from './travelPlanningTypes';

export function generateItinerary(
  input: TravelPreferences | string,
  history: ChatMessage[] = [],
  options: GenerateItineraryOptions = {},
): Promise<Itinerary> {
  return generateItineraryWithStream(input, history, options)
    .catch(() => generateItineraryWithBackend(input, history, options));
}
