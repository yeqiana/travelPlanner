import React, { useState, useEffect, useRef } from 'react';
import { AnimatePresence } from 'motion/react';
import { AppTopBar } from './components/AppTopBar';
import { HomeForm } from '../pages/home/HomeForm';
import { ChatView } from '../pages/chat/ChatView';
import { SessionSidebar } from '../features/chat-session/components/SessionSidebar';
import { SessionSearchModal } from '../features/chat-session/components/SessionSearchModal';
import { SettingsModal } from '../features/user-preferences/components/SettingsModal';
import { TravelPreferences, ChatMessage, ChatSession, UserPreferences } from '../shared/types/travel';
import { generateItinerary } from '../features/travel-planning/api/travelPlanningApi';

const MOCK_SESSIONS: ChatSession[] = [
  {
    id: 'mock-1',
    title: '三亚 3天经典游',
    updatedAt: Date.now(),
    history: [
      { id: 'm1-1', role: 'user', text: '我想去三亚玩3天，主打休闲，和伴侣一起，希望看日落、吃海鲜。' },
      { id: 'm1-2', role: 'assistant', text: '没问题，为您精心定制了三亚浪漫之旅：', itinerary: {
          title: "三亚3天2晚浪漫海岛游",
          summary: "享受阳光沙滩，漫步椰梦长廊，品尝鲜美海鲜，留下最美回忆。",
          days: [
            { dayNumber: 1, theme: "椰风海韵与绝美落日", activities: [
                { time: "14:00", description: "到达三亚，办理酒店入住稍作休息", location: "三亚湾度假酒店", duration: "1小时", priceEstimate: { hotelMin: 300, hotelMax: 1200, restaurantMin: 50, restaurantMax: 150 } },
                { time: "17:00", description: "漫步椰梦长廊，欣赏三亚最美日落", location: "椰梦长廊", duration: "2小时", transportationToNext: "打车", priceEstimate: { hotelMin: 200, hotelMax: 800, restaurantMin: 30, restaurantMax: 100 } },
                { time: "19:00", description: "逛第一市场，挑选新鲜海鲜并加工品尝", location: "第一市场", duration: "2小时", priceEstimate: { hotelMin: 150, hotelMax: 500, restaurantMin: 100, restaurantMax: 300 } }
            ]},
            { dayNumber: 2, theme: "免税买买买与海滨漫步", activities: [
                { time: "10:00", description: "前往国际免税城", location: "海棠湾", duration: "4小时", transportationToNext: "打车", priceEstimate: { hotelMin: 800, hotelMax: 3000, restaurantMin: 80, restaurantMax: 500 } },
                { time: "15:00", description: "亚龙湾沙滩漫步，体验水上项目", location: "亚龙湾沙滩", duration: "3小时", priceEstimate: { hotelMin: 600, hotelMax: 2500, restaurantMin: 60, restaurantMax: 200 } }
            ]}
          ],
          tips: ["三亚紫外线强，务必带好高倍防晒霜和墨镜。","买海鲜时注意货比三家。"]
        }
      }
    ]
  },
  {
    id: 'mock-2',
    title: '呼伦贝尔草原 5天游',
    updatedAt: Date.now() - 100000,
    history: [
      { id: 'm2-1', role: 'user', text: '想去呼伦贝尔大草原放松5天，带爸妈去，节奏稍微慢一点。' },
      { id: 'm2-2', role: 'assistant', text: '为您规划了适合带长辈游玩的呼伦贝尔慢节奏行程：', itinerary: {
          title: "呼伦贝尔5日原生态慢享之旅",
          summary: "策马奔腾共享人世繁华，品尝烤全羊，深度体验蒙古族文化风情。",
          days: [
            { dayNumber: 1, theme: "初识大草原", activities: [
                { time: "11:00", description: "抵达海拉尔，接机后前往酒店休息", location: "海拉尔市区", duration: "1小时", transportationToNext: "包车前往", priceEstimate: { hotelMin: 200, hotelMax: 600, restaurantMin: 40, restaurantMax: 120 } },
                { time: "14:00", description: "乘车前往天下第一曲水莫日格勒河", location: "莫日格勒河", duration: "3小时", priceEstimate: { hotelMin: 300, hotelMax: 800, restaurantMin: 60, restaurantMax: 200 } }
            ]}
          ],
          tips: ["草原昼夜温差大，带好保暖防风衣物。", "长辈出行注意备足常用药。"]
        }
      }
    ]
  }
];

export default function App() {
  const [sessions, setSessions] = useState<ChatSession[]>(() => {
    const saved = localStorage.getItem('travel_sessions');
    if (saved) {
      const parsed = JSON.parse(saved);
      // Migrate old sessions to have mock prices if they don't have them
      const migrated = parsed.map((session: ChatSession) => ({
        ...session,
        history: session.history.map(msg => {
          if (msg.itinerary) {
             return {
               ...msg,
               itinerary: {
                 ...msg.itinerary,
                 days: msg.itinerary.days.map(day => ({
                   ...day,
                   activities: day.activities.map(act => ({
                     ...act,
                     // Add mock data if priceEstimate is missing
                     priceEstimate: act.priceEstimate || { hotelMin: 200, hotelMax: 800, restaurantMin: 50, restaurantMax: 200 }
                   }))
                 }))
               }
             }
          }
          return msg;
        })
      }));
      if (migrated.length > 0) return migrated;
    }
    return MOCK_SESSIONS;
  });
  
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(sessions[0]?.id || null);
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [showSettingsModal, setShowSettingsModal] = useState(false);
  const [showSearchModal, setShowSearchModal] = useState(false);

  const [userPrefs, setUserPrefs] = useState<UserPreferences>(() => {
    const saved = localStorage.getItem('agent_travel_user_prefs');
    if (saved) return JSON.parse(saved);
    return { travelStyle: '适中节奏', accommodationType: '舒适型酒店', diningPreference: '当地特色美食', dietaryRestrictions: '' };
  });

  // Long press tracking for sessions
  const [actionSessionId, setActionSessionId] = useState<string | null>(null);
  const [isEditingTitle, setIsEditingTitle] = useState(false);
  const [editTitleVal, setEditTitleVal] = useState('');
  const pressTimer = useRef<NodeJS.Timeout | undefined>(undefined);

  useEffect(() => {
    localStorage.setItem('travel_sessions', JSON.stringify(sessions));
  }, [sessions]);

  useEffect(() => {
    localStorage.setItem('agent_travel_user_prefs', JSON.stringify(userPrefs));
  }, [userPrefs]);

  const activeSession = sessions.find(s => s.id === currentSessionId);
  const history = activeSession?.history || [];

  const handleInitialGenerate = async (prefs: TravelPreferences) => {
    const newSessionId = Date.now().toString();
    const initialPrompt = `我想去 ${prefs.destinations} 玩 ${prefs.days} 天，主打${prefs.vibe}，和${prefs.companions}一起。
【我的长期旅行偏好】：
- 行程节奏：${userPrefs.travelStyle}
- 住宿要求：${userPrefs.accommodationType}
- 餐饮偏好：${userPrefs.diningPreference}
${userPrefs.dietaryRestrictions ? `- 饮食禁忌：${userPrefs.dietaryRestrictions}\n` : ''}
${prefs.additionalNotes ? '【本次特别说明】：' + prefs.additionalNotes : ''}`;
    
    const initialUserMsg: ChatMessage = {
      id: Date.now().toString(),
      role: 'user',
      text: initialPrompt,
    };

    const loadingMsg: ChatMessage = {
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      text: '',
      isLoading: true
    };

    const newSession: ChatSession = {
      id: newSessionId,
      title: `${prefs.destinations} ${prefs.days}天游`,
      updatedAt: Date.now(),
      history: [initialUserMsg, loadingMsg],
    };

    setSessions(prev => [newSession, ...prev]);
    setCurrentSessionId(newSessionId);

    try {
      const result = await generateItinerary(initialPrompt, [], { sessionId: newSessionId });
      setSessions(prev => prev.map(s => {
        if (s.id === newSessionId) {
          return {
            ...s,
            history: s.history.map(msg => 
              msg.id === loadingMsg.id 
              ? { ...msg, isLoading: false, text: result.assistantReply || '为您定制的行程如下：', itinerary: result }
              : msg
            )
          };
        }
        return s;
      }));
    } catch (e) {
      setSessions(prev => prev.map(s => {
        if (s.id === newSessionId) {
          return {
            ...s,
            history: s.history.map(msg => 
              msg.id === loadingMsg.id 
              ? { ...msg, isLoading: false, text: '行程生成失败，请检查网络或后重试。' }
              : msg
            )
          };
        }
        return s;
      }));
    }
  };

  const handleSendFollowUp = async (text: string) => {
    if (!currentSessionId) return;

    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      role: 'user',
      text
    };
    
    const loadingMsg: ChatMessage = {
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      text: '',
      isLoading: true
    };

    setSessions(prev => prev.map(s => {
      if (s.id === currentSessionId) {
        return {
          ...s,
          updatedAt: Date.now(),
          history: [...s.history, userMsg, loadingMsg]
        };
      }
      return s;
    }));

    setSessions((prev: ChatSession[]) => {
      const updatedSession = prev.find(s => s.id === currentSessionId);
      if (!updatedSession) return prev;
      
      const newHistory = updatedSession.history.slice(0, -1); 

      generateItinerary(text, newHistory, { sessionId: currentSessionId })
      .then(result => {
        setSessions(currentSessions => currentSessions.map(s => {
          if (s.id === currentSessionId) {
            return {
              ...s,
              title: s.title,
              history: s.history.map(m => 
                m.id === loadingMsg.id 
                ? { ...m, isLoading: false, text: result.assistantReply || '已为您更新行程：', itinerary: result }
                : m
              )
            };
          }
          return s;
        }));
      }).catch(err => {
        setSessions(currentSessions => currentSessions.map(s => {
          if (s.id === currentSessionId) {
            return {
              ...s,
              history: s.history.map(m => 
                m.id === loadingMsg.id 
                ? { ...m, isLoading: false, text: '生成失败，请重试。' }
                : m
              )
            };
          }
          return s;
        }));
      });

      return prev;
    });
  };

  const handleNewChat = () => {
    setCurrentSessionId(null);
    setIsSidebarOpen(false);
  };

  const handleSelectSession = (id: string) => {
    setCurrentSessionId(id);
    setIsSidebarOpen(false);
  };

  const handleDeleteSession = (id: string) => {
    setSessions(prev => prev.filter(s => s.id !== id));
    if (currentSessionId === id) setCurrentSessionId(null);
    setActionSessionId(null);
  };

  const startPress = (e: React.TouchEvent | React.MouseEvent, s: ChatSession) => {
    pressTimer.current = setTimeout(() => {
      setActionSessionId(s.id);
      setIsEditingTitle(false);
      setEditTitleVal(s.title);
    }, 600);
  };

  const clearPress = () => {
    if (pressTimer.current) clearTimeout(pressTimer.current);
  };

  const handleSaveSessionTitle = (id: string) => {
    setSessions(prev => prev.map(x => x.id === id ? { ...x, title: editTitleVal } : x));
    setActionSessionId(null);
  };

  const handleOpenSettings = () => {
    setShowSettingsModal(true);
    setIsSidebarOpen(false);
  };

  return (
    <div className="max-w-md mx-auto h-[100dvh] w-full bg-white text-gray-900 overflow-hidden relative shadow-2xl sm:border-x sm:border-gray-200 font-sans flex flex-col">
      
      <AppTopBar
        onOpenSidebar={() => setIsSidebarOpen(true)}
        onOpenSearch={() => setShowSearchModal(true)}
      />

      {/* Main Content Area */}
      <div className="flex-1 overflow-y-auto w-full flex flex-col relative bg-white min-h-0">
        {!currentSessionId ? (
          <HomeForm onGenerate={handleInitialGenerate} />
        ) : (
          <ChatView history={history} onSend={handleSendFollowUp} />
        )}
      </div>

      <SessionSidebar
        isOpen={isSidebarOpen}
        sessions={sessions}
        currentSessionId={currentSessionId}
        actionSessionId={actionSessionId}
        isEditingTitle={isEditingTitle}
        editTitleVal={editTitleVal}
        onClose={() => setIsSidebarOpen(false)}
        onNewChat={handleNewChat}
        onSelectSession={handleSelectSession}
        onDeleteSession={handleDeleteSession}
        onStartPress={startPress}
        onClearPress={clearPress}
        onStartEditTitle={() => setIsEditingTitle(true)}
        onChangeEditTitle={setEditTitleVal}
        onSaveTitle={handleSaveSessionTitle}
        onOpenSettings={handleOpenSettings}
        onClearActionSession={() => setActionSessionId(null)}
      />

      {/* Settings Modal */}
      <AnimatePresence>
        {showSettingsModal && (
          <SettingsModal 
            userPrefs={userPrefs} 
            onSave={(prefs) => { setUserPrefs(prefs); setShowSettingsModal(false); }} 
            onClose={() => setShowSettingsModal(false)} 
          />
        )}
      </AnimatePresence>

      <AnimatePresence>
        {showSearchModal && (
          <SessionSearchModal
            sessions={sessions}
            onSelect={(id) => {
              setCurrentSessionId(id);
              setShowSearchModal(false);
            }}
            onClose={() => setShowSearchModal(false)}
          />
        )}
      </AnimatePresence>
    </div>
  );
}
