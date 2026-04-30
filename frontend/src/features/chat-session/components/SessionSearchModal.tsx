import React, { useState } from 'react';
import { motion } from 'motion/react';
import { Search, X, MessageSquare, Calendar } from 'lucide-react';
import { ChatSession } from '../../../shared/types/travel';

export function SessionSearchModal({
  sessions,
  onSelect,
  onClose
}: {
  sessions: ChatSession[];
  onSelect: (id: string) => void;
  onClose: () => void;
}) {
  const [searchTerm, setSearchTerm] = useState('');

  const filteredSessions = sessions.filter(s => {
    if (!searchTerm) return false;
    const term = searchTerm.toLowerCase();
    
    // Search in title
    if (s.title.toLowerCase().includes(term)) return true;
    
    // Search in date
    if (new Date(s.updatedAt).toLocaleDateString().includes(term)) return true;
    
    // Search in itinerary data
    const hasMatchInHistory = s.history.some(msg => {
      // Check message text
      if (msg.text && msg.text.toLowerCase().includes(term)) return true;
      
      // Check itinerary destinations, theme, activities, etc.
      if (msg.itinerary) {
        if (msg.itinerary.title.toLowerCase().includes(term)) return true;
        if (msg.itinerary.summary.toLowerCase().includes(term)) return true;
        
        return msg.itinerary.days.some(day => {
          if (day.theme.toLowerCase().includes(term)) return true;
          return day.activities.some(act => 
            act.location.toLowerCase().includes(term) || 
            act.description.toLowerCase().includes(term)
          );
        });
      }
      return false;
    });

    return hasMatchInHistory;
  });

  return (
    <motion.div 
      initial={{ opacity: 0 }} 
      animate={{ opacity: 1 }} 
      exit={{ opacity: 0 }}
      className="fixed inset-0 z-[100] flex flex-col bg-white"
    >
      <div className="flex items-center gap-3 p-4 border-b border-gray-100 bg-white">
        <div className="flex-1 flex items-center bg-gray-100 rounded-full px-4 py-2 text-gray-700">
          <Search size={18} className="text-gray-400 mr-2 shrink-0" />
          <input 
            type="text" 
            placeholder="搜索记录 (地点、日期、关键词)..." 
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            className="bg-transparent border-none outline-none flex-1 text-[15px]"
            autoFocus
          />
          {searchTerm && (
            <button onClick={() => setSearchTerm('')} className="shrink-0 p-1">
              <X size={16} className="text-gray-400" />
            </button>
          )}
        </div>
        <button onClick={onClose} className="text-blue-500 hover:text-blue-600 font-semibold px-2 shrink-0 border-none bg-transparent">
          取消
        </button>
      </div>

      <div className="flex-1 overflow-y-auto w-full bg-gray-50 flex justify-center">
        <div className="max-w-md w-full p-4">
          {!searchTerm ? (
            <div className="text-center text-gray-400 mt-10 text-[14px]">请输入关键词搜索历史行程</div>
          ) : filteredSessions.length === 0 ? (
            <div className="text-center text-gray-400 mt-10 text-[14px]">未找到包含 "{searchTerm}" 的相关行程</div>
          ) : (
            <div className="space-y-3">
              {filteredSessions.map(s => (
                <button
                  key={s.id}
                  onClick={() => {
                    onSelect(s.id);
                    onClose();
                  }}
                  className="w-full text-left bg-white p-4 rounded-xl shadow-sm border border-gray-100 hover:border-blue-200 transition-colors"
                >
                  <div className="flex items-start gap-3">
                    <div className="mt-0.5 p-2 bg-blue-50 text-blue-500 rounded-lg shrink-0">
                      <MessageSquare size={18} />
                    </div>
                    <div className="flex-1 w-0">
                      <h3 className="font-bold text-gray-900 mb-1 truncate text-[15px]">{s.title}</h3>
                      <div className="flex items-center text-[12px] text-gray-400 gap-1 mt-1">
                        <Calendar size={12} className="shrink-0" />
                        <span className="truncate">
                          {new Date(s.updatedAt).toLocaleDateString()}
                        </span>
                      </div>
                    </div>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>
    </motion.div>
  );
}
