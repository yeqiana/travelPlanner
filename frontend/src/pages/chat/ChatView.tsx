import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Sparkles, CalendarPlus, ArrowRight, Map, Share2 } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { ChatMessage, Itinerary } from '../../shared/types/travel';
import { exportItineraryToICS } from '../../features/travel-planning/lib/export';
import { InteractiveItinerary } from '../../features/travel-planning/components/InteractiveItinerary';
import { RouteMapModal } from '../../features/travel-planning/components/RouteMapModal';
import { ItineraryShareModal } from '../../features/travel-planning/components/ItineraryShareModal';

export function ChatView({ history, onSend }: { history: ChatMessage[], onSend: (text: string) => void }) {
  const [inputVal, setInputVal] = useState('');
  const [routeMapData, setRouteMapData] = useState<Itinerary | null>(null);
  const [itineraryShareData, setItineraryShareData] = useState<Itinerary | null>(null);
  const endOfMessagesRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endOfMessagesRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [history]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputVal.trim()) return;
    onSend(inputVal.trim());
    setInputVal('');
  };

  return (
    <>
      <div className="flex-1 overflow-y-auto px-4 py-6 space-y-6 min-h-0 bg-white">
        {history.map((msg) => (
          <div key={msg.id} className="flex w-full flex-col">
            {msg.role === 'user' ? (
              <div className="flex justify-end mb-2">
                <div className="max-w-[85%] bg-blue-500 text-white px-5 py-3.5 rounded-2xl rounded-tr-sm text-[15px] leading-relaxed break-words whitespace-pre-wrap font-medium shadow-sm">
                  {msg.text}
                </div>
              </div>
            ) : (
              <div className="flex flex-col max-w-[100%] pb-4">
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-8 h-8 rounded-full bg-black text-white flex items-center justify-center shadow-sm flex-shrink-0">
                    <Sparkles size={16} />
                  </div>
                  <span className="font-bold text-gray-900 text-sm">AgentTravel</span>
                </div>
                <div className="text-[16px] leading-relaxed text-gray-900 px-1">
                  {msg.isLoading ? (
                    <div className="flex items-center gap-2 text-gray-400 h-6">
                      <div className="flex gap-1.5 opacity-80">
                        <motion.div animate={{ scale: [1, 1.2, 1], opacity: [0.5, 1, 0.5] }} transition={{ duration: 1, repeat: Infinity }} className="w-2.5 h-2.5 bg-gray-400 rounded-full" />
                        <motion.div animate={{ scale: [1, 1.2, 1], opacity: [0.5, 1, 0.5] }} transition={{ duration: 1, repeat: Infinity, delay: 0.2 }} className="w-2.5 h-2.5 bg-gray-400 rounded-full" />
                        <motion.div animate={{ scale: [1, 1.2, 1], opacity: [0.5, 1, 0.5] }} transition={{ duration: 1, repeat: Infinity, delay: 0.4 }} className="w-2.5 h-2.5 bg-gray-400 rounded-full" />
                      </div>
                    </div>
                  ) : (
                    <>
                      {msg.text && (
                        <div className="prose prose-sm w-full max-w-none text-gray-800 prose-p:leading-relaxed break-words">
                          <ReactMarkdown remarkPlugins={[remarkGfm]}>{msg.text}</ReactMarkdown>
                        </div>
                      )}
                      {msg.itinerary && (
                        <div className="mt-5 mb-2 flex flex-col gap-3">
                          <InteractiveItinerary itinerary={msg.itinerary} />
                          <div className="flex flex-row items-center justify-end gap-2 mt-2 px-1">
                            <button 
                              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-yellow-200 bg-yellow-50 text-yellow-700 shadow-sm transition-colors hover:bg-yellow-100"
                              onClick={() => setRouteMapData(msg.itinerary!)}
                              title="生成一图流"
                              aria-label="生成一图流"
                            >
                              <Map size={17} />
                            </button>
                            <button
                              onClick={() => setItineraryShareData(msg.itinerary!)}
                              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-purple-200 bg-purple-50 text-purple-700 shadow-sm transition-colors hover:bg-purple-100"
                              title="生成行程海报"
                              aria-label="生成行程海报"
                            >
                              <Share2 size={17} />
                            </button>
                            <button 
                              onClick={() => exportItineraryToICS(msg.itinerary!)}
                              className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full border border-blue-200 bg-blue-50 text-blue-700 shadow-sm transition-colors hover:bg-blue-100"
                              title="导出至日历"
                              aria-label="导出至日历"
                            >
                              <CalendarPlus size={17} />
                            </button>
                          </div>
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>
            )}
          </div>
        ))}
        <div ref={endOfMessagesRef} className="h-4" />
      </div>

      <div className="px-4 py-3 sticky bottom-0 z-30 pb-safe bg-gradient-to-t from-white via-white to-white/90 backdrop-blur-sm">
        <form onSubmit={handleSubmit} className="flex flex-col relative max-w-3xl mx-auto w-full pt-2">
          {!history[history.length - 1]?.isLoading && history[history.length - 1]?.role !== 'user' && (
            <div className="flex gap-2 overflow-x-auto no-scrollbar pb-3 mb-1">
              {['增加一天行程', '换一些餐厅', '太累了，减少些景点'].map((suggestion) => (
                <button
                  key={suggestion}
                  type="button"
                  onClick={() => onSend(suggestion)}
                  className="shrink-0 bg-blue-50 text-blue-600 hover:bg-blue-100 px-4 py-2 rounded-full text-[13px] font-bold border border-blue-100 transition-colors whitespace-nowrap"
                >
                  {suggestion}
                </button>
              ))}
            </div>
          )}
          <div className="relative flex items-end w-full border border-gray-300 bg-[#f4f4f5] shadow-sm rounded-[24px] focus-within:ring-2 focus-within:ring-blue-500/20 focus-within:border-blue-500/50 transition-all overflow-hidden duration-200">
            <textarea
              value={inputVal}
              onChange={(e) => setInputVal(e.target.value)}
              placeholder="发送消息更新行程..."
              className="flex-1 min-w-0 bg-transparent px-5 py-3.5 text-[15px] outline-none font-medium resize-none min-h-[52px] max-h-[150px] m-0 w-full placeholder:text-gray-400 text-gray-900"
              rows={1}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault();
                  handleSubmit(e);
                }
              }}
            />
            <div className="p-2 flex-shrink-0 flex items-center justify-center mb-0.5 mr-1">
              <button
                type="submit"
                disabled={!inputVal.trim() || history[history.length - 1]?.isLoading}
                className="w-10 h-10 rounded-full bg-blue-500 text-white flex items-center justify-center disabled:opacity-30 disabled:bg-gray-400 transition-all active:scale-95"
              >
                <ArrowRight size={20} strokeWidth={2.5} />
              </button>
            </div>
          </div>
        </form>
      </div>

      <AnimatePresence>
        {routeMapData && (
          <RouteMapModal itinerary={routeMapData} onClose={() => setRouteMapData(null)} />
        )}
        {itineraryShareData && (
          <ItineraryShareModal itinerary={itineraryShareData} onClose={() => setItineraryShareData(null)} />
        )}
      </AnimatePresence>
    </>
  );
}
