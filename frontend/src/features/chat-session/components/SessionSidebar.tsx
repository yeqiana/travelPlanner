import React from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Plus, MessageSquare, X, Trash2, Edit2, Check, Settings, User, LogOut } from 'lucide-react';
import { ChatSession } from '../../../shared/types/travel';

interface SessionSidebarProps {
  isOpen: boolean;
  sessions: ChatSession[];
  currentSessionId: string | null;
  actionSessionId: string | null;
  isEditingTitle: boolean;
  editTitleVal: string;
  onClose: () => void;
  onNewChat: () => void;
  onSelectSession: (id: string) => void;
  onDeleteSession: (id: string) => void;
  onStartPress: (event: React.TouchEvent | React.MouseEvent, session: ChatSession) => void;
  onClearPress: () => void;
  onStartEditTitle: () => void;
  onChangeEditTitle: (title: string) => void;
  onSaveTitle: (id: string) => void;
  onOpenSettings: () => void;
  onClearActionSession: () => void;
}

export function SessionSidebar({
  isOpen,
  sessions,
  currentSessionId,
  actionSessionId,
  isEditingTitle,
  editTitleVal,
  onClose,
  onNewChat,
  onSelectSession,
  onDeleteSession,
  onStartPress,
  onClearPress,
  onStartEditTitle,
  onChangeEditTitle,
  onSaveTitle,
  onOpenSettings,
  onClearActionSession,
}: SessionSidebarProps) {
  return (
    <>
      <AnimatePresence>
        {isOpen && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={onClose}
              className="absolute inset-0 z-40 bg-black/40 backdrop-blur-sm"
            />
            <motion.div
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'spring', bounce: 0, duration: 0.3 }}
              className="absolute inset-y-0 left-0 z-50 w-[80%] max-w-[320px] bg-[#f9f9f9] border-r border-gray-200 flex flex-col shadow-2xl"
            >
              <div className="p-4 pt-12 flex justify-between items-center bg-[#f9f9f9]">
                <h2 className="text-xl font-bold tracking-tight text-gray-900">历史行程</h2>
                <button
                  onClick={onClose}
                  className="p-2 bg-white rounded-full shadow-sm text-gray-500 border border-gray-200 active:scale-95 transition-transform"
                >
                  <X size={18} />
                </button>
              </div>

              <div className="flex-1 overflow-y-auto px-4 py-4 space-y-1">
                <button
                  onClick={onNewChat}
                  className="w-full flex items-center gap-3 px-4 py-3.5 rounded-xl bg-blue-500 text-white shadow-md shadow-blue-500/20 active:scale-95 transition-all mb-6 focus:outline-none"
                >
                  <Plus size={20} />
                  <span className="font-semibold text-[15px]">新建行程规划</span>
                </button>

                <div className="text-xs font-bold text-gray-500 uppercase tracking-wider pb-2 mb-2 border-b border-gray-200">
                  最近（长按可编辑）
                </div>
                {sessions.length === 0 ? (
                  <div className="text-sm text-gray-400 mt-4 text-center">暂无历史记录</div>
                ) : (
                  sessions.map(s => (
                    <div key={s.id} className="relative">
                      <button
                        onMouseDown={(event) => onStartPress(event, s)}
                        onMouseUp={onClearPress}
                        onMouseLeave={onClearPress}
                        onTouchStart={(event) => onStartPress(event, s)}
                        onTouchEnd={onClearPress}
                        onContextMenu={(event) => { event.preventDefault(); onStartPress(event as any, s); }}
                        onClick={() => onSelectSession(s.id)}
                        className={`w-full flex items-center gap-3 px-3 py-3 rounded-xl transition-colors text-left focus:outline-none ${currentSessionId === s.id ? 'bg-gray-200' : 'hover:bg-gray-100'}`}
                      >
                        <MessageSquare size={18} className={currentSessionId === s.id ? 'text-gray-900' : 'text-gray-500'} />
                        <span className={`flex-1 truncate text-[15px] ${currentSessionId === s.id ? 'font-semibold text-gray-900' : 'text-gray-700'}`}>
                          {s.title}
                        </span>
                      </button>

                      <AnimatePresence>
                        {actionSessionId === s.id && (
                          <motion.div
                            initial={{ opacity: 0, scale: 0.95 }}
                            animate={{ opacity: 1, scale: 1 }}
                            exit={{ opacity: 0, scale: 0.95 }}
                            className="absolute z-50 left-0 right-0 top-12 mt-1 bg-white border border-gray-200 rounded-xl shadow-xl overflow-hidden"
                          >
                            {isEditingTitle ? (
                              <div className="p-3 flex gap-2 items-center bg-gray-50">
                                <input
                                  type="text"
                                  value={editTitleVal}
                                  onChange={event => onChangeEditTitle(event.target.value)}
                                  className="flex-1 min-w-0 bg-white border border-gray-300 rounded-md px-2 py-1.5 text-sm outline-none text-gray-900"
                                  autoFocus
                                />
                                <button
                                  onClick={() => onSaveTitle(s.id)}
                                  className="p-1.5 bg-blue-500 text-white rounded-md"
                                >
                                  <Check size={16} />
                                </button>
                              </div>
                            ) : (
                              <div className="flex flex-col py-1">
                                <button
                                  onClick={(event) => { event.stopPropagation(); onStartEditTitle(); }}
                                  className="flex items-center gap-3 px-4 py-2.5 text-sm font-medium text-gray-700 hover:bg-gray-100"
                                >
                                  <Edit2 size={16} /> 重命名
                                </button>
                                <hr className="border-t border-gray-100 my-1" />
                                <button
                                  onClick={(event) => { event.stopPropagation(); onDeleteSession(s.id); }}
                                  className="flex items-center gap-3 px-4 py-2.5 text-sm font-medium text-red-500 hover:bg-red-50"
                                >
                                  <Trash2 size={16} /> 删除记录
                                </button>
                              </div>
                            )}
                          </motion.div>
                        )}
                      </AnimatePresence>
                    </div>
                  ))
                )}
              </div>

              <div className="p-4 border-t border-gray-200 bg-white">
                <div className="flex items-center gap-3 mb-4 px-2">
                  <div className="w-10 h-10 rounded-full bg-blue-500 text-white flex items-center justify-center shadow-md border-2 border-white ring-2 ring-blue-50">
                    <User size={20} />
                  </div>
                  <div className="flex flex-col min-w-0">
                    <span className="text-sm font-bold text-gray-900 tracking-tight truncate">798737426@qq.com</span>
                    <span className="text-[11px] text-blue-600 font-extrabold uppercase tracking-wider bg-blue-50 w-fit px-2 py-0.5 rounded-md mt-0.5">Free Plan</span>
                  </div>
                </div>

                <div className="space-y-1">
                  <button
                    onClick={onOpenSettings}
                    className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm text-gray-700 font-semibold hover:bg-gray-100 transition-colors active:scale-[0.98]"
                  >
                    <Settings size={18} className="text-gray-500" />
                    偏好设置
                  </button>
                  <button
                    className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm text-red-600 font-semibold hover:bg-red-50 transition-colors active:scale-[0.98]"
                  >
                    <LogOut size={18} className="text-red-400" />
                    退出登录
                  </button>
                </div>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {actionSessionId && (
        <div className="fixed inset-0 z-40" onClick={onClearActionSession} />
      )}
    </>
  );
}
