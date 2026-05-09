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
      plainMarkdown: assistantReply,
      dialogIntent: response.dialogIntent || null,
      contextualSuggestions: response.contextualSuggestions || [],
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
    plainMarkdown: buildAssistantReply(plan, imageBrief),
    dialogIntent: response.dialogIntent || null,
    contextualSuggestions: response.contextualSuggestions || [],
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
  const parsed = parseSegment(description);
  return {
    time: parsed.time || time,
    location: parsed.location || city || '待确认',
    description: parsed.description || description,
    duration: parsed.duration || (time === '晚上' ? '晚上' : '半天'),
    transportationToNext: parsed.transportationToNext,
  };
}

function parseSegment(value: string): Partial<Activity> {
  const normalized = value.trim();
  const time = normalized.match(/^(\d{1,2}[：:]\d{2}\s*-\s*\d{1,2}[：:]\d{2})/)?.[1]
    ?.replace(/：/g, ':')
    .replace(/\s+/g, '');
  return {
    time,
    location: fieldValue(normalized, '地点'),
    description: fieldValue(normalized, '安排'),
    transportationToNext: fieldValue(normalized, '交通'),
    duration: fieldValue(normalized, '耗时'),
  };
}

function fieldValue(value: string, field: string): string | undefined {
  const match = value.match(new RegExp(`${field}：([^；。]+)`));
  return match?.[1]?.trim();
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
  return mergeRelatedTips(uniqueStrings(tips));
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
  return mergeSecondConfirmRisks(uniqueStrings([
    ...(response.imageBrief?.riskTags || []),
    ...(response.risks || []),
    ...(plan?.risks || []),
  ]));
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

function mergeRelatedTips(tips: string[]): string[] {
  const groups = {
    secondConfirm: [] as string[],
    transport: [] as string[],
    hotel: [] as string[],
    attraction: [] as string[],
    preparation: [] as string[],
  };
  const preserved: string[] = [];

  for (const tip of tips) {
    if (isRiskTip(tip)) {
      preserved.push(tip);
    } else if (isEvidenceSecondConfirmTip(tip)) {
      groups.secondConfirm.push(secondConfirmLabel(tip));
    } else if (isTransportTip(tip)) {
      groups.transport.push(tip);
    } else if (isHotelTip(tip)) {
      groups.hotel.push(tip);
    } else if (isAttractionTip(tip)) {
      groups.attraction.push(tip);
    } else if (isPreparationTip(tip)) {
      groups.preparation.push(tip);
    } else if (isGeneralSecondConfirmTip(tip)) {
      groups.secondConfirm.push(secondConfirmLabel(tip));
    } else {
      preserved.push(tip);
    }
  }

  return uniqueStrings([
    secondConfirmTipSummary(groups.secondConfirm),
    transportTipSummary(groups.transport),
    hotelTipSummary(groups.hotel),
    attractionTipSummary(groups.attraction),
    preparationTipSummary(groups.preparation),
    ...preserved,
  ]);
}

function isRiskTip(tip: string): boolean {
  return tip.includes('风险') && !tip.includes('二次确认');
}

function isGeneralSecondConfirmTip(tip: string): boolean {
  return tip.includes('二次确认') || tip.includes('票务/门票确认');
}

function isEvidenceSecondConfirmTip(tip: string): boolean {
  return /^.+?\s*(交通|天气|住宿|景点|路线)证据\s*信息需二次确认[。.]?$/.test(tip);
}

function isTransportTip(tip: string): boolean {
  return containsAnyText(tip, '车票', '机票', '班次', '换乘', '路线/交通', '跨城交通', '大交通');
}

function isHotelTip(tip: string): boolean {
  return containsAnyText(tip, '酒店', '住宿', '房态', '取消政策', '退改规则');
}

function isAttractionTip(tip: string): boolean {
  return containsAnyText(tip, '景点', '预约', '开放时间', '入园规则', '门票');
}

function isPreparationTip(tip: string): boolean {
  return containsAnyText(tip, '证件', '充电器', '雨具', '常用药', '出发前准备');
}

function containsAnyText(value: string, ...keywords: string[]): boolean {
  return keywords.some(keyword => value.includes(keyword));
}

function transportTipSummary(tips: string[]): string | null {
  if (tips.length === 0) return null;
  return '票务交通：确认往返和跨城车票/机票，并核对路线、班次、换乘和退改规则。';
}

function hotelTipSummary(tips: string[]): string | null {
  if (tips.length === 0) return null;
  return '酒店住宿：确认酒店价格、位置、房态和取消政策。';
}

function attractionTipSummary(tips: string[]): string | null {
  if (tips.length === 0) return null;
  return '景点预约：确认热门景点预约、开放时间、门票和入园规则。';
}

function preparationTipSummary(tips: string[]): string | null {
  if (tips.length === 0) return null;
  return '行前准备：检查证件、充电器、雨具和常用药。';
}

function secondConfirmTipSummary(tips: string[]): string | null {
  const labels = uniqueStrings(tips).filter(label => label.length > 0);
  if (labels.length === 0) return null;
  return secondConfirmSummary(labels);
}

function mergeSecondConfirmRisks(risks: string[]): string[] {
  const confirmLabels: string[] = [];
  const otherRisks: string[] = [];

  for (const risk of risks) {
    if (isSecondConfirmRisk(risk)) {
      confirmLabels.push(secondConfirmLabel(risk));
    } else {
      otherRisks.push(risk);
    }
  }

  const labels = uniqueStrings(confirmLabels).filter(label => label.length > 0);
  if (labels.length === 0) return otherRisks;

  return [secondConfirmSummary(labels), ...otherRisks];
}

function isSecondConfirmRisk(risk: string): boolean {
  return risk.includes('二次确认');
}

function secondConfirmLabel(risk: string): string {
  if (risk.includes('车票/酒店价格')) return '车票/酒店价格';
  if (risk.includes('景点预约')) return '景点预约';

  const evidenceMatch = risk.match(/^(.+?)\s*(交通|天气|住宿|景点|路线)证据\s*信息需二次确认[。.]?$/);
  if (evidenceMatch) {
    return `${evidenceMatch[1].trim()}${evidenceMatch[2]}信息`;
  }

  return risk
    .replace(/[。.]$/u, '')
    .replace(/信息需二次确认/u, '信息')
    .replace(/需二次确认/u, '')
    .trim();
}

function secondConfirmSummary(labels: string[]): string {
  const visibleLabels = labels.slice(0, 6);
  const suffix = labels.length > visibleLabels.length ? '等' : '';
  return `出发前统一二次确认：${visibleLabels.join('、')}${suffix}。`;
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
