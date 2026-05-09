import React, { useState } from 'react';
import { motion } from 'motion/react';
import { MapPin, Sparkles } from 'lucide-react';
import { TravelPreferences } from '../../shared/types/travel';

export function HomeForm({ onGenerate }: { onGenerate: (prefs: TravelPreferences) => void }) {
  const [departureCity, setDepartureCity] = useState('');
  const [dateText, setDateText] = useState('');
  const [destinations, setDestinations] = useState('');
  const [days, setDays] = useState(3);
  const [vibe, setVibe] = useState('放松休闲');
  const [companions, setCompanions] = useState('情侣/夫妻');
  const [peopleCount, setPeopleCount] = useState(2);
  const [budget, setBudget] = useState('');
  const [transportPreference, setTransportPreference] = useState('');
  const [hotelPreference, setHotelPreference] = useState('');
  const [diningPreference, setDiningPreference] = useState('');
  const [pace, setPace] = useState('适中节奏');
  const [mustVisit, setMustVisit] = useState('');
  const [avoidPlaces, setAvoidPlaces] = useState('');
  const [notes, setNotes] = useState('');
  const [submitError, setSubmitError] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const missingFields = requiredMissingFields();
    if (missingFields.length > 0) {
      setSubmitError(`请先补充：${missingFields.join('、')}`);
      return;
    }
    setSubmitError('');
    onGenerate({
      departureCity: departureCity.trim(),
      dateText: dateText.trim(),
      destinations: destinations.trim(),
      days,
      vibe,
      companions,
      peopleCount,
      budget: budget.trim(),
      transportPreference: transportPreference.trim(),
      hotelPreference: hotelPreference.trim(),
      diningPreference: diningPreference.trim(),
      pace,
      mustVisit: mustVisit.trim(),
      avoidPlaces: avoidPlaces.trim(),
      additionalNotes: notes.trim(),
    });
  };

  const vibes = ['放松休闲', '特种兵打卡', '历史人文', '自然风光', '美食吃货'];
  const companionOptions = ['独自一人', '情侣/夫妻', '带娃冲刺', '带长辈', '朋友结伴'];
  const paceOptions = ['轻松慢游', '适中节奏', '紧凑多玩'];

  const inputBgClass = "bg-[#f4f4f5] border border-transparent focus-within:border-blue-400 focus-within:bg-white focus-within:shadow-sm";

  const requiredMissingFields = () => {
    const fields: string[] = [];
    if (!departureCity.trim()) fields.push('出发城市');
    if (!dateText.trim()) fields.push('出发时间');
    if (!destinations.trim()) fields.push('目的地');
    if (days <= 0) fields.push('出行天数');
    return fields;
  };

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
        <div className="grid grid-cols-1 gap-4">
          <div className="space-y-2.5">
            <label className="text-[15px] font-bold text-gray-900 flex items-center gap-2 pl-1">
              从哪里出发？
            </label>
            <div className={`relative rounded-[20px] overflow-hidden transition-all ${inputBgClass}`}>
              <div className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400">
                <MapPin size={20} />
              </div>
              <input
                type="text"
                placeholder="如：西安、北京、上海"
                value={departureCity}
                onChange={(e) => setDepartureCity(e.target.value)}
                className="w-full pl-12 pr-4 py-4 bg-transparent outline-none font-semibold text-gray-900 text-[16px] placeholder:text-gray-400 placeholder:font-normal"
                aria-required="true"
              />
            </div>
          </div>

          <div className="space-y-2.5">
            <label className="text-[15px] font-bold text-gray-900 flex items-center gap-2 pl-1">
              什么时候出发？
            </label>
            <div className={`rounded-[20px] overflow-hidden transition-all ${inputBgClass}`}>
              <input
                type="text"
                placeholder="如：五一、周末、2026-05-01"
                value={dateText}
                onChange={(e) => setDateText(e.target.value)}
                className="w-full px-4 py-4 bg-transparent outline-none font-semibold text-gray-900 text-[16px] placeholder:text-gray-400 placeholder:font-normal"
                aria-required="true"
              />
            </div>
          </div>
        </div>

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
              aria-required="true"
            />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-2.5">
            <label className="text-[15px] font-bold text-gray-900 flex items-center gap-2 pl-1">
              人数
            </label>
            <div className={`rounded-[20px] overflow-hidden transition-all ${inputBgClass}`}>
              <input
                type="number"
                min={1}
                value={peopleCount}
                onChange={(e) => setPeopleCount(Math.max(1, Number(e.target.value)))}
                className="w-full px-4 py-4 bg-transparent outline-none font-semibold text-gray-900 text-[16px]"
              />
            </div>
          </div>
          <div className="space-y-2.5">
            <label className="text-[15px] font-bold text-gray-900 flex items-center gap-2 pl-1">
              总预算
            </label>
            <div className={`rounded-[20px] overflow-hidden transition-all ${inputBgClass}`}>
              <input
                type="text"
                placeholder="如：5000元"
                value={budget}
                onChange={(e) => setBudget(e.target.value)}
                className="w-full px-4 py-4 bg-transparent outline-none font-semibold text-gray-900 text-[16px] placeholder:text-gray-400 placeholder:font-normal"
              />
            </div>
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
            行程节奏
          </label>
          <div className="flex flex-wrap gap-2">
            {paceOptions.map(option => (
              <button
                key={option}
                type="button"
                onClick={() => setPace(option)}
                className={`px-4 py-2.5 rounded-full text-[14px] font-semibold transition-all ${
                  pace === option
                    ? 'bg-blue-500 text-white shadow-md'
                    : `bg-[#f4f4f5] text-gray-700 hover:bg-gray-300`
                }`}
              >
                {option}
              </button>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-1 gap-3">
          <div className={`rounded-[20px] overflow-hidden transition-all ${inputBgClass}`}>
            <input
              type="text"
              placeholder="交通偏好，如：高铁优先、可自驾"
              value={transportPreference}
              onChange={(e) => setTransportPreference(e.target.value)}
              className="w-full px-4 py-3.5 bg-transparent outline-none font-semibold text-gray-900 text-[15px] placeholder:text-gray-400 placeholder:font-normal"
            />
          </div>
          <div className={`rounded-[20px] overflow-hidden transition-all ${inputBgClass}`}>
            <input
              type="text"
              placeholder="酒店偏好，如：地铁旁、亲子酒店"
              value={hotelPreference}
              onChange={(e) => setHotelPreference(e.target.value)}
              className="w-full px-4 py-3.5 bg-transparent outline-none font-semibold text-gray-900 text-[15px] placeholder:text-gray-400 placeholder:font-normal"
            />
          </div>
          <div className={`rounded-[20px] overflow-hidden transition-all ${inputBgClass}`}>
            <input
              type="text"
              placeholder="餐饮偏好，如：当地小吃、清淡、不吃辣"
              value={diningPreference}
              onChange={(e) => setDiningPreference(e.target.value)}
              className="w-full px-4 py-3.5 bg-transparent outline-none font-semibold text-gray-900 text-[15px] placeholder:text-gray-400 placeholder:font-normal"
            />
          </div>
          <div className={`rounded-[20px] overflow-hidden transition-all ${inputBgClass}`}>
            <input
              type="text"
              placeholder="必去景点，如：西湖、外滩"
              value={mustVisit}
              onChange={(e) => setMustVisit(e.target.value)}
              className="w-full px-4 py-3.5 bg-transparent outline-none font-semibold text-gray-900 text-[15px] placeholder:text-gray-400 placeholder:font-normal"
            />
          </div>
          <div className={`rounded-[20px] overflow-hidden transition-all ${inputBgClass}`}>
            <input
              type="text"
              placeholder="想避开的内容，如：排队太久、爬山"
              value={avoidPlaces}
              onChange={(e) => setAvoidPlaces(e.target.value)}
              className="w-full px-4 py-3.5 bg-transparent outline-none font-semibold text-gray-900 text-[15px] placeholder:text-gray-400 placeholder:font-normal"
            />
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
          {submitError && (
            <div className="mb-3 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-[13px] font-bold leading-relaxed text-red-600">
              {submitError}
            </div>
          )}
          <button
            type="submit"
            className="w-full bg-black text-white rounded-2xl py-4 font-bold text-lg hover:bg-gray-900 active:scale-[0.98] transition-all flex items-center justify-center gap-2 shadow-lg"
          >
            开始生成
          </button>
        </div>
      </form>
    </motion.div>
  );
}
