import { MapPin } from 'lucide-react';

export function RouteLine({ route }: { route?: string[] }) {
  const cities = route?.filter(Boolean) || [];
  if (cities.length === 0) return null;

  return (
    <section className="space-y-3">
      <div className="flex items-center gap-2 text-[13px] font-black text-gray-900">
        <MapPin size={15} className="text-blue-500" />
        <span>路线</span>
      </div>
      <div className="flex flex-wrap gap-2">
        {cities.map((city, index) => (
          <div key={`${city}-${index}`} className="flex max-w-full items-center gap-2">
            <span className="max-w-[140px] break-words rounded-full bg-blue-50 px-3 py-1.5 text-[12px] font-bold text-blue-700">
              {city}
            </span>
            {index < cities.length - 1 && <span className="text-gray-300">→</span>}
          </div>
        ))}
      </div>
    </section>
  );
}
