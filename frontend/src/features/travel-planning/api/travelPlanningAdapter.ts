import { Activity, DayPlan, Itinerary } from '../../../shared/types/travel';
import { BackendDailyPlan, TravelPlanResponse } from './travelPlanningTypes';

export function adaptTravelPlanResponse(response: TravelPlanResponse): Itinerary {
  if (response.needClarification) {
    const questions = clarificationQuestions(response);
    const assistantReply = [
      '### 需要补充信息',
      '',
      ...questions.map(question => `- ${question}`),
    ].join('\n');
    return {
      title: '需要补充信息',
      summary: questions.join(' '),
      days: [],
      tips: questions,
      assistantReply,
    };
  }

  if (!response.recommendedPlan) {
    throw new Error('后端未返回推荐行程。');
  }

  const plan = response.recommendedPlan;
  return {
    title: plan.title || '旅行计划',
    summary: plan.summary || '已为你生成旅行计划。',
    days: (plan.dailyPlans || []).map(adaptDailyPlan),
    tips: collectTips(response),
    assistantReply: buildAssistantReply(plan),
  };
}

function buildAssistantReply(plan: NonNullable<TravelPlanResponse['recommendedPlan']>): string {
  const title = plan.title || '旅行计划';
  const summary = plan.summary || '已为你生成旅行计划。';
  return [`### ${title}`, '', summary].join('\n');
}

function adaptDailyPlan(day: BackendDailyPlan): DayPlan {
  return {
    dayNumber: day.day,
    theme: day.city ? `第${day.day}天 ${day.city}` : `第${day.day}天`,
    activities: [
      adaptActivity('上午', day.city, day.morning),
      adaptActivity('下午', day.city, day.afternoon),
      adaptActivity('晚上', day.city, day.evening),
    ].filter((activity): activity is Activity => Boolean(activity)),
  };
}

function adaptActivity(time: string, city: string | undefined, description?: string): Activity | null {
  if (!description) return null;
  return {
    time,
    location: city || '待确认',
    description,
    duration: time === '晚上' ? '晚上' : '半天',
  };
}

function clarificationQuestions(response: TravelPlanResponse): string[] {
  const structured = response.structuredClarificationQuestions
    ?.map(question => question.question)
    .filter(Boolean) || [];
  const plain = response.clarificationQuestions || [];
  const questions = structured.length > 0 ? structured : plain;
  return questions.length > 0 ? questions : ['请补充更多旅行信息。'];
}

function collectTips(response: TravelPlanResponse): string[] {
  const plan = response.recommendedPlan;
  const tips = [
    ...(plan?.risks || []),
    ...(plan?.todoList || []),
    ...(response.risks || []),
    ...(response.reminders || []).map(formatReminder).filter(Boolean),
  ];
  return Array.from(new Set(tips));
}

function formatReminder(reminder: unknown): string {
  if (typeof reminder === 'string') return reminder;
  if (!reminder || typeof reminder !== 'object') return '';
  const value = reminder as Record<string, unknown>;
  const candidate = value.content || value.message || value.title || value.description;
  return typeof candidate === 'string' ? candidate : '';
}
