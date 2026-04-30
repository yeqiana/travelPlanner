import React, { useRef, useState } from 'react';
import { motion } from 'motion/react';
import { X, Sparkles, MapPin, Clock, Share2 } from 'lucide-react';
import { toBlob } from 'html-to-image';
import { Activity, DayPlan } from '../../../shared/types/travel';

function formatTime(timeStr: string) {
  if (!timeStr) return '未知';
  const match = timeStr.match(/(\d{1,2})[：:](\d{2})/);
  if (match) {
    return `${match[1].padStart(2, '0')}:${match[2]}`;
  }
  return timeStr;
}

export function LocationShareModal({ activity, day, onClose }: { activity: Activity, day: DayPlan, onClose: () => void }) {
  const posterRef = useRef<HTMLDivElement>(null);
  const [isGenerating, setIsGenerating] = useState(false);

  const handleShare = async () => {
    if (!posterRef.current) return;
    setIsGenerating(true);
    try {
      const blob = await toBlob(posterRef.current, { backgroundColor: '#ffffff', pixelRatio: 2 });
      if (!blob) throw new Error('Failed to create blob');
      
      const file = new File([blob], `${activity.location}.png`, { type: 'image/png' });
      
      if (navigator.canShare && navigator.canShare({ files: [file] })) {
        try {
          await navigator.share({
            title: activity.location,
            text: activity.description,
            files: [file]
          });
        } catch (err) {
          console.log('Share canceled or failed', err);
        }
      } else {
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = `${activity.location}-打卡.png`;
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
        
        <div ref={posterRef} className="bg-white p-7 relative overflow-hidden rounded-[24px] aspect-[4/5] flex flex-col justify-between shadow-2xl tracking-tight text-gray-900 border border-gray-100">
          <div className="absolute top-0 right-0 w-56 h-56 bg-blue-100 rounded-full mix-blend-multiply opacity-60 translate-x-1/3 -translate-y-1/3 blur-3xl"></div>
          <div className="absolute bottom-0 left-0 w-48 h-48 bg-yellow-100 rounded-full mix-blend-multiply opacity-60 -translate-x-1/3 translate-y-1/3 blur-3xl"></div>
          
          <div className="relative z-10 space-y-4">
            <div className="flex flex-wrap items-center gap-2 mb-2">
              <div className="bg-black text-white text-[11px] font-black px-2.5 py-1 rounded-md uppercase tracking-wider shadow-sm">Day {day.dayNumber}</div>
              <div className="text-[13px] font-bold text-gray-500 leading-snug break-words min-w-0">{day.theme}</div>
            </div>
            
            <h2 className="text-[28px] min-[380px]:text-[32px] font-black mb-1 leading-tight tracking-tight mt-6 break-words">{activity.location}</h2>
            
            <div className="flex flex-wrap items-center gap-1.5 text-[14px] font-bold text-blue-600 bg-blue-50/80 backdrop-blur-sm w-fit px-2.5 py-1.5 rounded-lg border border-blue-100">
              <Clock size={16} /> 预计 {formatTime(activity.time)} · {activity.duration}
            </div>
            
            <p className="text-[16px] leading-[1.6] text-gray-700 font-medium mt-6 line-clamp-6">
              {activity.description}
            </p>
            
            {activity.priceEstimate && (
              <div className="flex flex-col gap-2 mt-4 pt-4 border-t border-gray-100/60">
                <div className="flex items-center gap-2 text-[13px] font-bold text-rose-700">
                  <span className="bg-rose-100/80 p-1 rounded">酒店估算</span>
                  <span className="break-words">¥{activity.priceEstimate.hotelMin} - ¥{activity.priceEstimate.hotelMax}</span>
                </div>
                <div className="flex items-center gap-2 text-[13px] font-bold text-amber-700">
                  <span className="bg-amber-100/80 p-1 rounded">餐饮估算</span>
                  <span className="break-words">¥{activity.priceEstimate.restaurantMin} - ¥{activity.priceEstimate.restaurantMax}</span>
                </div>
              </div>
            )}
          </div>
          
          <div className="relative z-10 flex items-center justify-between border-t border-gray-100 pt-5 mt-4">
            <div className="flex items-center gap-1.5">
              <div className="w-6 h-6 rounded-md bg-blue-500 flex items-center justify-center text-white">
                <Sparkles size={12} />
              </div>
              <span className="text-[12px] font-extrabold text-gray-400 tracking-wider">AgentTravel</span>
            </div>
            <MapPin size={18} className="text-gray-300" />
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
              处理中...
            </span>
          ) : (
            <>
              <Share2 size={20} />
              分享此地
            </>
          )}
        </button>
      </motion.div>
      </div>
    </motion.div>
  );
}
