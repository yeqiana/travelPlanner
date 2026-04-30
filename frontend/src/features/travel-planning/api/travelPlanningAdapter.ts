import { Activity, BudgetCard, DayCard, DayPlan, Itinerary, ReminderCard } from '../../../shared/types/travel';
import {
  BackendDailyPlan,
  BackendImageBrief,
  BackendTravelPlan,
  BackendTravelReminder,
  TravelPlanResponse,
} from './travelPlanningTypes';

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
  const imageBrief = response.imageBrief;
  return {
    title: imageBrief?.title || plan.title || '旅行计划',
    summary: imageBrief?.subtitle || plan.summary || '已为你生成旅行计划。',
    days: (plan.dailyPlans || []).map(adaptDailyPlan),
    tips: collectTips(response),
    assistantReply: buildAssistantReply(plan, imageBrief),
    routeLine: collectRouteLine(response),
    dayCards: collectDayCards(response),
    riskTags: collectRiskTags(response),
    reminderCards: collectReminderCards(response),
    budgetCards: collectBudgetCards(response),
    footerNote: imageBrief?.footerNote || undefined,
  };
}

function buildAssistantReply(plan: BackendTravelPlan, imageBrief?: BackendImageBrief | null): string {
  const title = imageBrief?.title || plan.title || '旅行计划';
  const summary = imageBrief?.subtitle || plan.summary || '已为你生成旅行计划。';
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

function collectRouteLine(response: TravelPlanResponse): string[] {
  return nonEmptyStrings(response.imageBrief?.routeLine || response.recommendedPlan?.route || []);
}

function collectDayCards(response: TravelPlanResponse): DayCard[] {
  const imageCards = response.imageBrief?.dayCards
    ?.map(card => ({
      day: card.day,
      title: card.title || `Day ${card.day}`,
      items: nonEmptyStrings(card.items || []),
    }))
    .filter(card => card.items.length > 0) || [];

  if (imageCards.length > 0) return imageCards;

  return response.recommendedPlan?.dailyPlans
    ?.map(day => ({
      day: day.day,
      title: day.city ? `Day ${day.day} ${day.city}` : `Day ${day.day}`,
      items: nonEmptyStrings([day.morning, day.afternoon, day.evening]),
    }))
    .filter(card => card.items.length > 0) || [];
}

function collectRiskTags(response: TravelPlanResponse): string[] {
  const plan = response.recommendedPlan;
  return uniqueStrings([
    ...(response.imageBrief?.riskTags || []),
    ...(response.risks || []),
    ...(plan?.risks || []),
  ]);
}

function collectReminderCards(response: TravelPlanResponse): ReminderCard[] {
  const imageCards = response.imageBrief?.reminderCards
    ?.map(card => ({
      title: card.title || '',
      time: card.time,
    }))
    .filter(card => card.title) || [];

  const reminderCards = (response.reminders || [])
    .map(formatReminderCard)
    .filter((card): card is ReminderCard => Boolean(card));

  const todoCards = (response.recommendedPlan?.todoList || []).map(title => ({
    title,
    time: '出发前确认',
  }));

  return uniqueReminderCards([...imageCards, ...reminderCards, ...todoCards]);
}

function collectBudgetCards(response: TravelPlanResponse): BudgetCard[] {
  const imageCards = response.imageBrief?.budgetCards
    ?.map(card => ({
      name: card.name || '',
      value: card.value || '',
    }))
    .filter(card => card.name && card.value) || [];

  if (imageCards.length > 0) return imageCards;

  const estimate = response.recommendedPlan?.budgetEstimate;
  if (!estimate) return [];

  return Object.entries(estimate)
    .filter(([, value]) => typeof value === 'number' || typeof value === 'string')
    .map(([name, value]) => ({ name: budgetName(name), value: String(value) }));
}

function formatReminder(reminder: BackendTravelReminder): string {
  return formatReminderCard(reminder)?.title || '';
}

function formatReminderCard(reminder: BackendTravelReminder): ReminderCard | null {
  if (!reminder || typeof reminder !== 'object') return null;
  const title = reminder.title || reminder.description || '';
  if (!title) return null;
  return {
    title,
    time: reminder.remindRule || reminder.type,
  };
}

function nonEmptyStrings(values: Array<string | null | undefined>): string[] {
  return values.map(value => value?.trim()).filter((value): value is string => Boolean(value));
}

function uniqueStrings(values: Array<string | null | undefined>): string[] {
  return Array.from(new Set(nonEmptyStrings(values)));
}

function uniqueReminderCards(cards: ReminderCard[]): ReminderCard[] {
  const seen = new Set<string>();
  return cards.filter(card => {
    const key = `${card.title}|${card.time || ''}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function budgetName(name: string): string {
  const names: Record<string, string> = {
    total: '合计',
    transport: '交通',
    hotel: '住宿',
    attraction: '景点',
    mealAndLocal: '餐饮',
  };
  return names[name] || name;
}
