import React, { useState } from 'react';
import { Compass, Clock, Share2, BedDouble, Utensils, Navigation, Info, Sparkles } from 'lucide-react';
import { AnimatePresence } from 'motion/react';
import { Itinerary, Activity, DayPlan } from '../../../shared/types/travel';
import { LocationShareModal } from './LocationShareModal';
import { TravelPlanOverview } from './TravelPlanOverview';

function formatTime(timeStr: string) {
  if (!timeStr) return '未知';
  const match = timeStr.match(/(\d{1,2})[：:](\d{2})/);
  if (match) {
    return `${match[1].padStart(2, '0')}:${match[2]}`;
  }
  return timeStr;
}

export function InteractiveItinerary({ itinerary }: { itinerary: Itinerary }) {
  const [shareTarget, setShareTarget] = useState<{act: Activity, day: DayPlan} | null>(null);

  return (
    <>
      <div className="w-full bg-white rounded-3xl overflow-hidden shadow-xl shadow-gray-200/50 border border-gray-100">
        <div className="bg-gradient-to-b from-blue-50/80 to-white px-6 py-6 border-b border-gray-100">
          <div className="flex items-start mb-4">
            <div className="inline-flex flex-col items-center justify-center p-3 w-12 h-12 bg-white rounded-2xl shadow-sm border border-blue-100 text-blue-500">
              <Compass size={24} />
            </div>
          </div>
          <h2 className="text-[20px] font-black text-gray-900 tracking-tight leading-snug mb-2">{itinerary.title}</h2>
          <p className="text-[14px] text-gray-500 leading-relaxed font-medium">{itinerary.summary}</p>
        </div>

        <TravelPlanOverview itinerary={itinerary} />
        
        <div className="max-h-[65vh] overflow-y-auto px-4 py-5 space-y-8">
          {itinerary.days.map((day, dIdx) => (
            <div key={day.dayNumber} className="relative">
              <div className="bg-white py-3 mb-3 -mx-4 px-4 border-b border-gray-100/70">
                <div className="flex items-center gap-3">
                  <div className="bg-blue-500 text-white px-3 py-1.5 rounded-lg flex flex-col items-center justify-center shadow-md shadow-blue-500/20">
                     <span className="text-[9px] font-bold uppercase leading-none opacity-90 mb-0.5">Day</span>
                     <span className="text-[16px] font-black leading-none">{day.dayNumber}</span>
                  </div>
                  <h3 className="font-extrabold text-gray-900 text-[17px] leading-snug break-words min-w-0">{day.theme}</h3>
                </div>
              </div>

              <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">
                <table className="w-full table-fixed border-collapse text-left">
                  <thead className="bg-gray-50 text-[12px] font-black text-gray-500">
                    <tr>
                      <th className="w-[23%] border-b border-gray-200 px-3 py-2.5">时间</th>
                      <th className="w-[62%] border-b border-gray-200 px-3 py-2.5">安排</th>
                      <th className="w-[15%] border-b border-gray-200 px-2 py-2.5 text-center">操作</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {day.activities.map((act, aIdx) => (
                      <tr key={aIdx} className="align-top">
                        <td className="bg-blue-50/40 px-3 py-3 text-[13px] font-black text-blue-700">
                          <div className="leading-snug break-words">{formatTime(act.time)}</div>
                          <div className="mt-1 flex items-center gap-1 text-[11px] font-bold text-blue-500/80">
                            <Clock size={11} />
                            <span className="break-words">{act.duration}</span>
                          </div>
                        </td>
                        <td className="px-3 py-3">
                          <div className="text-[15px] font-black leading-snug text-gray-900 break-words">{act.location}</div>
                          <p className="mt-1.5 text-[13px] font-medium leading-relaxed text-gray-600 break-words">{act.description}</p>

                          {act.transportationToNext && (
                            <div className="mt-2 inline-flex max-w-full items-center gap-1.5 rounded-md bg-blue-50 px-2 py-1 text-[11px] font-bold text-blue-700">
                              <Navigation size={12} className="shrink-0" />
                              <span className="break-words">{act.transportationToNext}</span>
                            </div>
                          )}

                          {act.priceEstimate && (
                            <div className="mt-2 flex flex-col gap-1.5">
                              <div className="inline-flex max-w-full items-center gap-1.5 rounded-md border border-rose-100 bg-rose-50 px-2 py-1 text-[11px] font-bold text-rose-700">
                                <BedDouble size={12} className="shrink-0 text-rose-500" />
                                <span className="break-words">酒店 ¥{act.priceEstimate.hotelMin}-¥{act.priceEstimate.hotelMax}</span>
                              </div>
                              <div className="inline-flex max-w-full items-center gap-1.5 rounded-md border border-amber-100 bg-amber-50 px-2 py-1 text-[11px] font-bold text-amber-700">
                                <Utensils size={12} className="shrink-0 text-amber-500" />
                                <span className="break-words">餐饮 ¥{act.priceEstimate.restaurantMin}-¥{act.priceEstimate.restaurantMax}</span>
                              </div>
                            </div>
                          )}
                        </td>
                        <td className="px-2 py-3 text-center">
                          <button
                            onClick={() => setShareTarget({ act, day })}
                            className="mx-auto flex h-8 w-8 items-center justify-center rounded-full bg-gray-50 text-gray-400 transition-colors hover:bg-blue-50 hover:text-blue-500"
                            title="生成打卡图"
                            aria-label="生成打卡图"
                          >
                            <Share2 size={15} />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ))}
          
          {itinerary.tips.length > 0 && (
            <div className="bg-orange-50/70 rounded-xl p-4 border border-orange-100 mt-6 mb-2 relative overflow-hidden">
              <div className="absolute top-0 right-0 p-3 opacity-10">
                <Info size={42} />
              </div>
              <h3 className="text-orange-900 text-[14px] font-extrabold mb-3 flex items-center gap-1.5 relative z-10">
                <Sparkles size={15} className="text-orange-500"/> 出行必备贴士
              </h3>
              <ul className="space-y-2 relative z-10">
                {itinerary.tips.map((tip, idx) => (
                  <li key={idx} className="flex gap-2.5 items-start">
                    <div className="w-5 h-5 rounded-full bg-orange-200/50 flex-shrink-0 flex items-center justify-center text-orange-700 font-extrabold text-[11px] mt-0.5">
                      {idx + 1}
                    </div>
                    <p className="text-[13px] text-orange-900/80 leading-relaxed font-medium pt-0.5">{tip}</p>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>
      
      <AnimatePresence>
        {shareTarget && (
          <LocationShareModal 
            activity={shareTarget.act} 
            day={shareTarget.day} 
            onClose={() => setShareTarget(null)} 
          />
        )}
      </AnimatePresence>
    </>
  );
}
