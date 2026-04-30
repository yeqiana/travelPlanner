import React, { useState } from 'react';
import { motion } from 'motion/react';
import { MapPin, Sparkles } from 'lucide-react';
import { TravelPreferences } from '../../shared/types/travel';

export function HomeForm({ onGenerate }: { onGenerate: (prefs: TravelPreferences) => void }) {
  const [destinations, setDestinations] = useState('');
  const [days, setDays] = useState(3);
  const [vibe, setVibe] = useState('放松休闲');
  const [companions, setCompanions] = useState('情侣/夫妻');
  const [notes, setNotes] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onGenerate({ destinations, days, vibe, companions, additionalNotes: notes });
  };

  const vibes = ['放松休闲', '特种兵打卡', '历史人文', '自然风光', '美食吃货'];
  const companionOptions = ['独自一人', '情侣/夫妻', '带娃冲刺', '带长辈', '朋友结伴'];

  const inputBgClass = "bg-[#f4f4f5] border border-transparent focus-within:border-blue-400 focus-within:bg-white focus-within:shadow-sm";

  return (
    <motion.div
      initial={{ opacity: 0, y: 10 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -10 }}
      className="flex flex-col h-full px-5 py-2 overflow-y-auto"
    >
      <div className="flex flex-col items-center justify-center pt-8 pb-8">
        <div className="w-16 h-16 bg-blue-500 rounded-3xl flex items-center justify-center shadow-lg shadow-blue-500/20 mb-4">
          <Sparkles size={28} className="text-white" />
        </div>
        <h2 className="text-2xl font-bold text-gray-900 tracking-tight">告诉 AgentTravel</h2>
        <p className="text-gray-500 mt-2 text-sm font-medium">你需要去哪里的专属行程？</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6 pb-12 px-1">
        <div className="space-y-2.5">
          <label className="text-[15px] font-bold text-gray-900 flex items-center gap-2 pl-1">
             想去哪里？
          </label>
          <div className={`relative rounded-[20px] overflow-hidden transition-all ${inputBgClass}`}>
            <div className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400">
              <MapPin size={20} />
            </div>
            <input 
              type="text" 
              placeholder="如：杭州、成都或 江浙沪周边"
              value={destinations}
              onChange={(e) => setDestinations(e.target.value)}
              className="w-full pl-12 pr-4 py-4 bg-transparent outline-none font-semibold text-gray-900 text-[16px] placeholder:text-gray-400 placeholder:font-normal"
              required
            />
          </div>
        </div>

        <div className="space-y-2.5">
          <label className="text-[15px] font-bold text-gray-900 flex items-center gap-2 pl-1">
            玩几天？
          </label>
          <div className={`flex items-center justify-between p-2.5 rounded-[20px] ${inputBgClass}`}>
            <button 
              type="button" 
              onClick={() => setDays(Math.max(1, days - 1))}
              className="w-12 h-12 rounded-xl bg-white text-gray-900 flex items-center justify-center hover:bg-gray-50 transition-all border border-gray-200 disabled:opacity-50 text-xl font-medium shadow-sm"
              disabled={days <= 1}
            >
              -
            </button>
            <span className="font-bold text-xl text-gray-900 w-16 text-center">{days} <span className="text-sm font-medium text-gray-500">天</span></span>
            <button 
              type="button" 
              onClick={() => setDays(Math.min(30, days + 1))}
              className="w-12 h-12 rounded-xl bg-white text-gray-900 flex items-center justify-center hover:bg-gray-50 transition-all border border-gray-200 disabled:opacity-50 text-xl font-medium shadow-sm"
              disabled={days >= 30}
            >
              +
            </button>
          </div>
        </div>

        <div className="space-y-2.5">
          <label className="text-[15px] font-bold text-gray-900 flex items-center gap-2 pl-1">
            偏好风格
          </label>
          <div className="flex flex-wrap gap-2">
            {vibes.map(v => (
              <button
                key={v}
                type="button"
                onClick={() => setVibe(v)}
                className={`px-4 py-2.5 rounded-full text-[14px] font-semibold transition-all ${
                  vibe === v 
                    ? 'bg-blue-500 text-white shadow-md' 
                    : `bg-[#f4f4f5] text-gray-700 hover:bg-gray-300`
                }`}
              >
                {v}
              </button>
            ))}
          </div>
        </div>

        <div className="space-y-2.5">
          <label className="text-[15px] font-bold text-gray-900 flex items-center gap-2 pl-1">
            出行标签
          </label>
          <div className="flex flex-wrap gap-2">
            {companionOptions.map(c => (
              <button
                key={c}
                type="button"
                onClick={() => setCompanions(c)}
                className={`px-4 py-2.5 rounded-full text-[14px] font-semibold transition-all ${
                  companions === c 
                     ? 'bg-blue-500 text-white shadow-md' 
                    : `bg-[#f4f4f5] text-gray-700 hover:bg-gray-300`
                }`}
              >
                {c}
              </button>
            ))}
          </div>
        </div>

        <div className="space-y-2.5">
          <label className="text-[15px] font-bold text-gray-900 flex items-center gap-2 pl-1">
            补充要求 (选填)
          </label>
          <div className={`rounded-[20px] p-4 transition-all ${inputBgClass}`}>
            <textarea
              placeholder="可以直接用口语化交流哦，比如：想多打卡些当地老字号小吃..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full bg-transparent outline-none font-medium text-gray-900 resize-none h-20 text-[15px] leading-relaxed placeholder:text-gray-400"
            />
          </div>
        </div>

        <div className="pt-4">
          <button
            type="submit"
            disabled={!destinations}
            className="w-full bg-black text-white rounded-2xl py-4 font-bold text-lg hover:bg-gray-900 active:scale-[0.98] transition-all disabled:opacity-50 flex items-center justify-center gap-2 shadow-lg"
          >
            开始生成
          </button>
        </div>
      </form>
    </motion.div>
  );
}
