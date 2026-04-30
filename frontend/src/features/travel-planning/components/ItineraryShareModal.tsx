import React, { useRef, useState } from 'react';
import { motion } from 'motion/react';
import { X, Sparkles, Share2, MapPin, Calendar, Clock } from 'lucide-react';
import { toBlob } from 'html-to-image';
import { Itinerary } from '../../../shared/types/travel';

export function ItineraryShareModal({ itinerary, onClose }: { itinerary: Itinerary, onClose: () => void }) {
  const posterRef = useRef<HTMLDivElement>(null);
  const [isGenerating, setIsGenerating] = useState(false);

  const handleShare = async () => {
    if (!posterRef.current) return;
    setIsGenerating(true);
    try {
      const blob = await toBlob(posterRef.current, { backgroundColor: '#ffffff', pixelRatio: 2 });
      if (!blob) throw new Error('Failed to create blob');
      
      const file = new File([blob], `${itinerary.title}.png`, { type: 'image/png' });
      
      if (navigator.canShare && navigator.canShare({ files: [file] })) {
        try {
          await navigator.share({
            title: itinerary.title,
            text: itinerary.summary,
            files: [file]
          });
        } catch (err) {
          console.log('Share canceled or failed', err);
        }
      } else {
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = `${itinerary.title}-行程海报.png`;
        link.click();
        URL.revokeObjectURL(url);
      }
      setIsGenerating(false);
    } catch (e) {
      console.error(e);
      alert('图片生成失败，请重试');
      setIsGenerating(false);
    }
  };

  const allDestinations = Array.from(new Set(itinerary.days.flatMap(d => d.activities.map(a => a.location)))).slice(0, 5);

  return (
    <motion.div 
      initial={{ opacity: 0 }} 
      animate={{ opacity: 1 }} 
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[60] overflow-y-auto bg-black/60"
    >
      <div className="min-h-full flex items-start justify-center p-4 sm:p-6">
        <motion.div 
          initial={{ y: 50, scale: 0.95 }}
          animate={{ y: 0, scale: 1 }}
          exit={{ y: 20, scale: 0.95 }}
          className="w-full max-w-sm flex flex-col gap-4 py-8 my-auto"
        >
        <div className="flex justify-end p-0">
          <button onClick={onClose} className="p-2 hover:bg-white/20 rounded-full text-white transition-colors border border-white/30 backdrop-blur-md">
            <X size={20} />
          </button>
        </div>
        
        <div ref={posterRef} className="bg-white p-7 relative overflow-hidden rounded-[24px] flex flex-col gap-6 shadow-2xl tracking-tight text-gray-900 border border-gray-100">
          <div className="absolute top-0 right-0 w-64 h-64 bg-blue-100 rounded-full mix-blend-multiply opacity-60 translate-x-1/3 -translate-y-1/3 blur-3xl"></div>
          <div className="absolute bottom-0 left-0 w-56 h-56 bg-yellow-100 rounded-full mix-blend-multiply opacity-60 -translate-x-1/3 translate-y-1/3 blur-3xl"></div>
          
          <div className="relative z-10">
            <div className="bg-blue-600 text-white text-[12px] font-black px-3 py-1.5 rounded-lg uppercase tracking-wider shadow-md w-fit mb-4">
              {itinerary.days.length} 天专属行程
            </div>
            
            <h2 className="text-[28px] min-[380px]:text-[32px] font-black mb-2 leading-tight tracking-tight break-words">{itinerary.title}</h2>
            <p className="text-[15px] font-medium text-gray-600 leading-relaxed">
              {itinerary.summary}
            </p>
          </div>

          <div className="relative z-10 bg-white/60 backdrop-blur-md border border-white/40 p-4 rounded-2xl shadow-sm space-y-3">
            <div className="text-[13px] font-bold text-gray-800 mb-2 flex items-center gap-1.5">
              <MapPin size={16} className="text-blue-500" />
              核心打卡地
            </div>
            <div className="flex flex-wrap gap-2">
              {allDestinations.map((loc, idx) => (
                <div key={idx} className="bg-blue-50 text-blue-700 font-semibold px-2.5 py-1 rounded-md text-[12px] break-words max-w-full">
                  {loc}
                </div>
              ))}
              {itinerary.days.flatMap(d => d.activities).length > 5 && (
                <div className="bg-gray-100 text-gray-600 font-semibold px-2.5 py-1 rounded-md text-[12px]">
                  等 {itinerary.days.flatMap(d => d.activities).length} 个地点
                </div>
              )}
            </div>
          </div>
          
          <div className="relative z-10 flex items-center justify-between border-t border-gray-100 pt-5 mt-2">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-xl bg-blue-500 flex items-center justify-center text-white shadow-md">
                <Sparkles size={16} />
              </div>
              <div>
                <div className="text-[14px] font-extrabold tracking-tight">AgentTravel</div>
                <div className="text-[10px] text-gray-400 font-medium">AI驱动的专属旅行管家</div>
              </div>
            </div>
            <div className="bg-blue-50 w-10 h-10 rounded-full flex items-center justify-center border border-blue-100">
               <span className="text-[12px] font-black text-blue-600">GO</span>
            </div>
          </div>
        </div>

        <button 
          onClick={handleShare}
          disabled={isGenerating}
          className="w-full mt-2 bg-blue-500 hover:bg-blue-600 text-white font-bold py-4 rounded-[20px] shadow-lg shadow-blue-500/30 flex justify-center items-center gap-2 disabled:opacity-50 transition-all active:scale-[0.98]"
        >
          {isGenerating ? (
            <span className="flex items-center gap-2">
              <motion.div animate={{ rotate: 360 }} transition={{ duration: 1, repeat: Infinity, ease: 'linear' }} className="w-5 h-5 border-2 border-white border-t-transparent rounded-full" />
              生成海报中...
            </span>
          ) : (
            <>
              <Share2 size={20} />
              分享行程海报
            </>
          )}
        </button>
      </motion.div>
      </div>
    </motion.div>
  );
}
