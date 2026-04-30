import { CalendarDays } from 'lucide-react';
import { DayCard } from '../../../shared/types/travel';

export function DayCardList({ days }: { days?: DayCard[] }) {
  const cards = days || [];
  if (cards.length === 0) return null;

  return (
    <section className="space-y-3">
      <div className="flex items-center gap-2 text-[13px] font-black text-gray-900">
        <CalendarDays size={15} className="text-blue-500" />
        <span>每日安排</span>
      </div>
      <div className="space-y-3">
        {cards.map(card => (
          <div key={`${card.day}-${card.title}`} className="border-l-2 border-blue-200 pl-3">
            <div className="break-words text-[13px] font-black text-gray-900">{card.title}</div>
            <ul className="mt-2 space-y-1.5">
              {card.items.map((item, index) => (
                <li key={`${item}-${index}`} className="break-words text-[12px] leading-relaxed text-gray-600">
                  {item}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </section>
  );
}
