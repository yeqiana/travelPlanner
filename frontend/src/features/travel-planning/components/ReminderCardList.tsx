import { Bell } from 'lucide-react';
import { ReminderCard } from '../../../shared/types/travel';

export function ReminderCardList({ reminders }: { reminders?: ReminderCard[] }) {
  const cards = reminders || [];
  if (cards.length === 0) return null;

  return (
    <section className="space-y-3">
      <div className="flex items-center gap-2 text-[13px] font-black text-gray-900">
        <Bell size={15} className="text-purple-500" />
        <span>提醒</span>
      </div>
      <div className="divide-y divide-purple-100 rounded-xl border border-purple-100 bg-purple-50/40">
        {cards.slice(0, 6).map((reminder, index) => (
          <div key={`${reminder.title}-${index}`} className="px-3 py-2.5">
            <div className="break-words text-[12px] font-black leading-snug text-purple-900">{reminder.title}</div>
            {reminder.time && (
              <div className="mt-1 break-words text-[11px] font-bold text-purple-500">{reminder.time}</div>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}
