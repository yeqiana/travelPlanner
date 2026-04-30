import { ShieldAlert } from 'lucide-react';

export function RiskTagList({ risks }: { risks?: string[] }) {
  const riskTags = risks || [];
  if (riskTags.length === 0) return null;

  return (
    <section className="space-y-3">
      <div className="flex items-center gap-2 text-[13px] font-black text-gray-900">
        <ShieldAlert size={15} className="text-orange-500" />
        <span>风险提示</span>
      </div>
      <div className="flex flex-wrap gap-2">
        {riskTags.map((risk, index) => (
          <span key={`${risk}-${index}`} className="max-w-full break-words rounded-full bg-orange-50 px-3 py-1.5 text-[12px] font-bold text-orange-700">
            {risk}
          </span>
        ))}
      </div>
    </section>
  );
}
