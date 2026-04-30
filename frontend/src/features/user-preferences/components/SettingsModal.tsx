import React, { useState } from 'react';
import { motion } from 'motion/react';
import { X, Settings } from 'lucide-react';
import { createPortal } from 'react-dom';
import { UserPreferences } from '../../../shared/types/travel';

export function SettingsModal({ 
  userPrefs, 
  onSave, 
  onClose 
}: { 
  userPrefs: UserPreferences, 
  onSave: (prefs: UserPreferences) => void, 
  onClose: () => void 
}) {
  const [prefs, setPrefs] = useState<UserPreferences>(userPrefs);

  return createPortal(
    <motion.div 
      initial={{ opacity: 0 }} 
      animate={{ opacity: 1 }} 
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/60 p-4"
    >
      <motion.div 
        initial={{ y: 50, scale: 0.95 }}
        animate={{ y: 0, scale: 1 }}
        exit={{ y: 20, scale: 0.95 }}
        className="bg-white rounded-3xl w-full max-w-md max-h-[90vh] flex flex-col overflow-hidden shadow-2xl"
      >
        <div className="flex justify-between items-center p-5 border-b border-gray-100 bg-white">
          <h3 className="font-bold text-gray-900 text-[18px] flex items-center gap-2">
            <Settings size={20} className="text-blue-500" />
            个人偏好设置
          </h3>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-full text-gray-500 transition-colors">
            <X size={20} />
          </button>
        </div>
        
        <div className="flex-1 overflow-y-auto p-6 space-y-6 bg-gray-50/50">
          <div>
            <label className="block text-[14px] font-bold text-gray-700 mb-2">旅行节奏偏好</label>
            <div className="grid grid-cols-1 min-[360px]:grid-cols-2 gap-3">
              {['紧凑打卡', '适中节奏', '深度慢游', '躺平度假'].map(style => (
                <button
                  key={style}
                  onClick={() => setPrefs({ ...prefs, travelStyle: style })}
                  className={`py-2.5 px-3 rounded-xl border text-[14px] font-medium transition-all ${
                    prefs.travelStyle === style 
                      ? 'bg-blue-50 border-blue-500 text-blue-700 shadow-sm shadow-blue-500/10' 
                      : 'bg-white border-gray-200 text-gray-600 hover:border-blue-200 hover:bg-blue-50/50'
                  }`}
                >
                  {style}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-[14px] font-bold text-gray-700 mb-2">住宿类型偏好</label>
            <div className="grid grid-cols-1 min-[360px]:grid-cols-2 gap-3">
              {['经济型便携酒店', '舒适型酒店', '豪华五星/度假村', '特色民宿', '青年旅舍'].map(acc => (
                <button
                  key={acc}
                  onClick={() => setPrefs({ ...prefs, accommodationType: acc })}
                  className={`py-2.5 px-3 rounded-xl border text-[14px] font-medium transition-all ${
                    prefs.accommodationType === acc 
                      ? 'bg-blue-50 border-blue-500 text-blue-700 shadow-sm shadow-blue-500/10' 
                      : 'bg-white border-gray-200 text-gray-600 hover:border-blue-200 hover:bg-blue-50/50'
                  }`}
                >
                  {acc}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-[14px] font-bold text-gray-700 mb-2">餐饮探店偏好</label>
            <div className="grid grid-cols-1 min-[360px]:grid-cols-2 gap-3">
              {['当地特色美食', '路边摊/夜市', '网红打卡餐饮', '高端米其林/精致餐饮', '随便吃点'].map(dining => (
                <button
                  key={dining}
                  onClick={() => setPrefs({ ...prefs, diningPreference: dining })}
                  className={`py-2.5 px-3 rounded-xl border text-[14px] font-medium transition-all ${
                    prefs.diningPreference === dining 
                      ? 'bg-blue-50 border-blue-500 text-blue-700 shadow-sm shadow-blue-500/10' 
                      : 'bg-white border-gray-200 text-gray-600 hover:border-blue-200 hover:bg-blue-50/50'
                  }`}
                >
                  {dining}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-[14px] font-bold text-gray-700 mb-2">饮食禁忌 (选填)</label>
            <input 
              type="text"
              placeholder="如：不吃香菜、海鲜过敏、素食主义..."
              value={prefs.dietaryRestrictions || ''}
              onChange={e => setPrefs({ ...prefs, dietaryRestrictions: e.target.value })}
              className="w-full bg-white border border-gray-200 rounded-xl px-4 py-3 text-[14px] text-gray-900 outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-all font-medium placeholder:text-gray-400 placeholder:font-normal"
            />
          </div>
        </div>

        <div className="p-5 border-t border-gray-100 bg-white">
          <button 
            onClick={() => onSave(prefs)}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-3.5 rounded-xl transition-colors shadow-lg shadow-blue-600/20 active:scale-[0.98]"
          >
            保存偏好设置
          </button>
        </div>
      </motion.div>
    </motion.div>,
    document.body
  );
}
