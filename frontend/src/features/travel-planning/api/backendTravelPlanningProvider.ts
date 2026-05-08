import { ChatMessage, Itinerary, TravelPreferences } from '../../../shared/types/travel';
import { adaptTravelPlanResponse } from './travelPlanningAdapter';
import { ApiResult, GenerateItineraryOptions, TravelPlanRequest, TravelPlanResponse } from './travelPlanningTypes';

export async function generateItineraryWithBackend(
  input: TravelPreferences | string,
  history: ChatMessage[] = [],
  options: GenerateItineraryOptions = {},
): Promise<Itinerary> {
  const request: TravelPlanRequest = {
    message: buildMessage(input, history),
    sessionId: options.sessionId,
  };

  const response = await fetch(`${apiBaseUrl()}/api/travel/plans`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error(`后端行程接口请求失败：${response.status}`);
  }

  const result = await response.json() as ApiResult<TravelPlanResponse>;
  if (result.code !== 0 || !result.data) {
    throw new Error(result.message || '后端行程接口返回失败。');
  }

  return adaptTravelPlanResponse(result.data);
}

function buildMessage(input: TravelPreferences | string, history: ChatMessage[]): string {
  if (typeof input === 'string') return input;

  const parts = [
    `我从${input.departureCity}出发`,
    `${input.dateText}去${input.destinations}玩${input.days}天`,
    `主打${input.vibe}`,
    `和${input.companions}一起`,
  ];
  if (input.peopleCount && input.peopleCount > 0) {
    parts.push(`${input.peopleCount}个人`);
  }
  if (input.budget) {
    parts.push(`总预算${input.budget}`);
  }
  if (input.transportPreference) {
    parts.push(`交通偏好${input.transportPreference}`);
  }
  if (input.hotelPreference) {
    parts.push(`酒店偏好${input.hotelPreference}`);
  }
  if (input.diningPreference) {
    parts.push(`餐饮偏好${input.diningPreference}`);
  }
  if (input.pace) {
    parts.push(`行程节奏${input.pace}`);
  }
  if (input.mustVisit) {
    parts.push(`必去景点：${input.mustVisit}`);
  }
  if (input.avoidPlaces) {
    parts.push(`想避开：${input.avoidPlaces}`);
  }
  if (input.additionalNotes) {
    parts.push(`补充要求：${input.additionalNotes}`);
  }

  const historyText = history
    .filter(message => !message.isLoading && message.text)
    .map(message => `${message.role === 'user' ? '用户' : 'AI'}：${message.text}`)
    .join('\n');

  return historyText ? `${parts.join('，')}。\n历史对话：\n${historyText}` : `${parts.join('，')}。`;
}

function apiBaseUrl(): string {
  const env = (import.meta as ImportMeta & { env?: Record<string, string | undefined> }).env;
  return (env?.VITE_API_BASE_URL || '').replace(/\/$/, '');
}
