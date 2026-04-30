import React, { useRef, useState } from 'react';
import { Sparkles, X, Compass, MapPin, Info, Download } from 'lucide-react';
import { motion } from 'motion/react';
import { toPng } from 'html-to-image';
import { Itinerary } from '../../../shared/types/travel';

export function RouteMapModal({ itinerary, onClose }: { itinerary: Itinerary, onClose: () => void }) {
  const posterRef = useRef<HTMLDivElement>(null);
  const [isGenerating, setIsGenerating] = useState(false);

  const downloadImage = async () => {
    if (!posterRef.current) return;
    setIsGenerating(true);
    try {
      const dataUrl = await toPng(posterRef.current, { backgroundColor: '#f0f9ff', pixelRatio: 2 });
      const link = document.createElement("a");
      link.href = dataUrl;
      link.download = `${itinerary.title}-行程路线图.png`;
      link.click();
    } catch (e) {
      console.error(e);
      alert('图片生成失败，请重试');
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0 }} 
      animate={{ opacity: 1 }} 
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
    >
      <motion.div 
        initial={{ y: 50, scale: 0.95 }}
        animate={{ y: 0, scale: 1 }}
        exit={{ y: 20, scale: 0.95 }}
        className="bg-gray-100 rounded-3xl w-full max-w-md max-h-[90vh] flex flex-col overflow-hidden shadow-2xl"
      >
        <div className="flex justify-between items-center p-4 border-b border-gray-200 bg-white">
          <h3 className="font-bold text-gray-800 text-lg flex items-center gap-2">
            <Sparkles size={18} className="text-yellow-500" /> 行程一图流
          </h3>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-full text-gray-500 transition-colors">
            <X size={20} />
          </button>
        </div>
        
        <div className="flex-1 overflow-y-auto p-4 bg-gray-50 relative">
          <div ref={posterRef} className="bg-gradient-to-br from-blue-50 to-indigo-50 p-6 shadow-sm font-sans relative overflow-hidden rounded-[24px]">
            {/* Decorative bg elements */}
            <div className="absolute top-0 right-0 w-32 h-32 bg-blue-200 rounded-full mix-blend-multiply filter blur-xl opacity-50 translate-x-1/2 -translate-y-1/2"></div>
            <div className="absolute bottom-0 left-0 w-32 h-32 bg-indigo-200 rounded-full mix-blend-multiply filter blur-xl opacity-50 -translate-x-1/2 translate-y-1/2"></div>
            
            <div className="relative z-10">
              <div className="flex items-center justify-center mb-6 pt-2">
                <div className="bg-white px-5 py-2.5 rounded-full shadow-sm border border-blue-100 inline-flex items-center gap-2 max-w-full">
                  <Compass size={22} className="text-blue-500 flex-shrink-0" />
                  <h2 className="text-[18px] font-black text-gray-800 tracking-tight leading-snug break-words min-w-0">{itinerary.title}</h2>
                </div>
              </div>
              
              <div className="bg-white/60 p-4 rounded-xl border border-white/60 shadow-sm backdrop-blur-sm mb-6 flex justify-center text-center">
                <p className="text-[13px] font-bold text-gray-700 leading-relaxed break-words">
                  {itinerary.summary}
                </p>
              </div>

              <div className="space-y-6">
                {itinerary.days.map((day, idx) => (
                  <div key={day.dayNumber} className="relative z-10">
                    {/* Connection line */}
                    {idx !== itinerary.days.length - 1 && (
                      <div className="absolute left-[22px] top-[40px] bottom-[-30px] w-0.5 border-l-2 border-dashed border-blue-300 z-0 opacity-60"></div>
                    )}
                    
                    <div className="flex gap-4 relative z-10">
                      {/* Day badge */}
                      <div className="w-12 h-12 shrink-0 rounded-full bg-blue-500 text-white flex flex-col items-center justify-center shadow-md border-[3px] border-white shadow-blue-500/30">
                        <span className="text-[9px] font-bold uppercase leading-none opacity-90 mt-0.5">Day</span>
                        <span className="text-[18px] font-black leading-tight">{day.dayNumber}</span>
                      </div>
                      
                      <div className="flex-1 bg-white rounded-2xl p-4 shadow-sm border border-blue-50 mt-1">
                        <h4 className="font-extrabold text-gray-800 text-[15px] leading-snug break-words mb-3">{day.theme}</h4>
                        <div className="space-y-6 ml-2 mt-2">
                          {day.activities.map((act, actIdx) => (
                            <div key={actIdx} className="relative flex gap-4 items-start">
                              {actIdx !== day.activities.length - 1 && (
                                <div className="absolute left-0 top-[24px] bottom-[-32px] w-0.5 border-l-2 border-dashed border-orange-200 z-0 opacity-70"></div>
                              )}
                              <div className="relative z-10 w-6 h-6 shrink-0 rounded-full bg-orange-50 border-2 border-orange-200 flex items-center justify-center -ml-[11px] mt-0.5 text-orange-500 shadow-[0_0_0_4px_white]">
                                <MapPin size={12} />
                              </div>
                              <div className="flex-1 min-w-0 pb-2">
                                <div className="text-[14px] font-bold text-gray-900 mb-1.5 tracking-tight flex items-center flex-wrap gap-2">
                                  <span className="break-words">{act.time} · {act.location}</span>
                                </div>
                                <div className="mb-2">
                                  <span className="text-[11px] bg-orange-100 text-orange-700 px-2 py-0.5 rounded-md font-bold tracking-wide whitespace-nowrap">{act.duration}</span>
                                </div>
                                <p className="text-[13px] text-gray-500 mt-2 leading-[1.6] break-words font-medium">{act.description}</p>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
              
              {itinerary.tips.length > 0 && (
                <div className="mt-8 bg-blue-600/5 rounded-xl p-4 border border-blue-600/10 backdrop-blur-sm relative z-10">
                  <div className="font-bold text-[13px] text-blue-700 mb-2 flex items-center gap-1.5">
                    <Info size={14} /> 行前提醒
                  </div>
                  <ul className="text-[12px] text-gray-600 space-y-2 pl-1">
                    {itinerary.tips.map((tip, idx) => (
                      <li key={idx} className="flex gap-1.5 leading-relaxed">
                        <span className="text-blue-400 font-bold flex-shrink-0">•</span> 
                        <span className="break-words">{tip}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
            
            <div className="mt-10 pt-4 border-t border-blue-900/5 text-center text-[10px] text-gray-400 font-semibold tracking-wider italic flex items-center justify-center gap-1 opacity-60 relative z-10">
              <Sparkles size={10} /> Created by AgentTravel
            </div>
          </div>
        </div>

        <div className="p-4 bg-white border-t border-gray-200 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)] z-20">
          <button 
            onClick={downloadImage}
            disabled={isGenerating}
            className="w-full bg-yellow-400 hover:bg-yellow-500 text-yellow-900 font-extrabold py-3.5 px-4 rounded-xl shadow-[0_4px_0_rgb(202,138,4)] hover:shadow-[0_2px_0_rgb(202,138,4)] hover:translate-y-[2px] transition-all flex justify-center items-center gap-2 disabled:opacity-50 disabled:shadow-none disabled:translate-y-[4px]"
          >
            {isGenerating ? (
              <span className="flex items-center gap-2">
                <motion.div animate={{ rotate: 360 }} transition={{ duration: 1, repeat: Infinity, ease: 'linear' }} className="w-5 h-5 border-2 border-yellow-900 border-t-transparent rounded-full" />
                生成中...
              </span>
            ) : (
              <>
                <Download size={20} strokeWidth={2.5} />
                保存一图流到相册
              </>
            )}
          </button>
        </div>
      </motion.div>
    </motion.div>
  );
}
