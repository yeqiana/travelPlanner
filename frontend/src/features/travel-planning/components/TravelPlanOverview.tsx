import { WalletCards } from 'lucide-react';
import { Itinerary } from '../../../shared/types/travel';
import { DayCardList } from './DayCardList';
import { ReminderCardList } from './ReminderCardList';
import { RiskTagList } from './RiskTagList';
import { RouteLine } from './RouteLine';

export function TravelPlanOverview({ itinerary }: { itinerary: Itinerary }) {
  const hasOverview = Boolean(
    itinerary.routeLine?.length ||
    itinerary.dayCards?.length ||
    itinerary.riskTags?.length ||
    itinerary.reminderCards?.length ||
    itinerary.budgetCards?.length,
  );

  if (!hasOverview) return null;

  return (
    <div className="space-y-5 border-b border-gray-100 px-4 py-5">
      <RouteLine route={itinerary.routeLine} />
      <DayCardList days={itinerary.dayCards} />
      <BudgetList itinerary={itinerary} />
      <RiskTagList risks={itinerary.riskTags} />
      <ReminderCardList reminders={itinerary.reminderCards} />
      {itinerary.footerNote && (
        <p className="text-[11px] font-medium leading-relaxed text-gray-400">{itinerary.footerNote}</p>
      )}
    </div>
  );
}

function BudgetList({ itinerary }: { itinerary: Itinerary }) {
  const cards = itinerary.budgetCards || [];
  if (cards.length === 0) return null;

  return (
    <section className="space-y-3">
      <div className="flex items-center gap-2 text-[13px] font-black text-gray-900">
        <WalletCards size={15} className="text-emerald-500" />
        <span>预算</span>
      </div>
      <div className="grid grid-cols-2 gap-2">
        {cards.map(card => (
          <div key={`${card.name}-${card.value}`} className="rounded-xl bg-emerald-50 px-3 py-2">
            <div className="text-[11px] font-bold text-emerald-600">{card.name}</div>
            <div className="mt-1 text-[14px] font-black text-emerald-900">{card.value}</div>
          </div>
        ))}
      </div>
    </section>
  );
}
