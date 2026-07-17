import React, { useState, useEffect, useRef } from 'react';
import { Message, Role, ChatSession, AVAILABLE_MODELS, ModelConfig } from './types';
import Sidebar from './components/Sidebar';
import MessageList from './components/MessageList';
import ModelSelector from './components/ModelSelector';
import { streamChatResponse } from './services/geminiService';
import { Menu, Send, Globe, Sparkles } from 'lucide-react';
import { v4 as uuidv4 } from 'uuid';

const STORAGE_KEY_HISTORY = 'xiao_zhou_ai_history';
const STORAGE_KEY_MODELS = 'xiao_zhou_ai_models_config';

const App: React.FC = () => {
  // State
  const [sessions, setSessions] = useState<ChatSession[]>([]);
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);
  const [input, setInput] = useState('');
  const [isSidebarOpen, setIsSidebarOpen] = useState(true); 
  const [isProcessing, setIsProcessing] = useState(false);
  const [enableSearch, setEnableSearch] = useState(true); // Default open based on prompt
  
  // Model State
  const [currentModelId, setCurrentModelId] = useState(AVAILABLE_MODELS[0].id);
  const [modelConfigs, setModelConfigs] = useState<ModelConfig[]>(AVAILABLE_MODELS);
  const [isModelSelectorOpen, setIsModelSelectorOpen] = useState(false);

  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Load History & Model Configs
  useEffect(() => {
    // History
    const savedHistory = localStorage.getItem(STORAGE_KEY_HISTORY);
    if (savedHistory) {
      try {
        setSessions(JSON.parse(savedHistory));
      } catch (e) { console.error("Failed to load history", e); }
    }

    // Model Configs (API Keys)
    const savedConfigs = localStorage.getItem(STORAGE_KEY_MODELS);
    if (savedConfigs) {
        try {
            const parsedConfigs = JSON.parse(savedConfigs);
            // Merge saved keys into default models list to keep structure updated
            setModelConfigs(prev => prev.map(m => {
                const saved = parsedConfigs.find((s: any) => s.id === m.id);
                return saved ? { ...m, userApiKey: saved.userApiKey } : m;
            }));
        } catch (e) { console.error("Failed to load config", e); }
    }

    if (window.innerWidth < 768) {
      setIsSidebarOpen(false);
    }
  }, []);

  // Save History
  useEffect(() => {
    localStorage.setItem(STORAGE_KEY_HISTORY, JSON.stringify(sessions));
  }, [sessions]);

  // Save Models
  useEffect(() => {
    localStorage.setItem(STORAGE_KEY_MODELS, JSON.stringify(modelConfigs));
  }, [modelConfigs]);

  // Adjust textarea height
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 150)}px`;
    }
  }, [input]);

  // Fix: Define currentSession as a variable so it is accessible in the JSX return
  const currentSession = sessions.find(s => s.id === currentSessionId);
  const currentModel = modelConfigs.find(m => m.id === currentModelId) || modelConfigs[0];
  
  const createNewSession = () => {
    const newSession: ChatSession = {
      id: uuidv4(),
      title: '新对话',
      messages: [],
      updatedAt: Date.now(),
      modelId: currentModelId
    };
    setSessions(prev => [newSession, ...prev]);
    setCurrentSessionId(newSession.id);
    return newSession;
  };

  const handleUpdateModelKey = (modelId: string, key: string) => {
    setModelConfigs(prev => prev.map(m => 
        m.id === modelId ? { ...m, userApiKey: key } : m
    ));
  };

  const handleSendMessage = async () => {
    if (!input.trim() || isProcessing) return;

    let session = currentSession;
    if (!session) {
      session = createNewSession();
    }

    const userMsg: Message = {
      id: uuidv4(),
      role: Role.USER,
      text: input.trim(),
      timestamp: Date.now()
    };

    const updatedMessages = [...session.messages, userMsg];
    
    // Auto-generate title
    let updatedTitle = session.title;
    if (session.messages.length === 0) {
      updatedTitle = userMsg.text.slice(0, 15) + (userMsg.text.length > 15 ? '...' : '');
    }

    setSessions(prev => prev.map(s => 
      s.id === session!.id 
        ? { ...s, messages: updatedMessages, title: updatedTitle, updatedAt: Date.now() } 
        : s
    ));
    
    setInput('');
    setIsProcessing(true);
    if (textareaRef.current) textareaRef.current.style.height = 'auto';

    const botMsgId = uuidv4();

    // Init bot message
    setSessions(prev => prev.map(s => 
        s.id === session!.id 
          ? { 
              ...s, 
              messages: [
                ...updatedMessages, 
                { id: botMsgId, role: Role.MODEL, text: '', timestamp: Date.now() }
              ] 
            } 
          : s
    ));

    // Stream
    await streamChatResponse(
      updatedMessages,
      currentModel, // Pass the full config which includes potential User API Key
      enableSearch,
      {
        onChunk: (text) => {
          setSessions(prev => prev.map(s => {
            if (s.id !== session!.id) return s;
            const msgs = s.messages.map(m => {
              if (m.id === botMsgId) {
                return { ...m, text: m.text + text };
              }
              return m;
            });
            return { ...s, messages: msgs };
          }));
        },
        onGrounding: (sources) => {
          setSessions(prev => prev.map(s => {
            if (s.id !== session!.id) return s;
            const msgs = s.messages.map(m => {
              if (m.id === botMsgId) {
                const existing = m.groundingSources || [];
                const newSources = sources.filter(ns => !existing.some(es => es.uri === ns.uri));
                return { ...m, groundingSources: [...existing, ...newSources] };
              }
              return m;
            });
            return { ...s, messages: msgs };
          }));
        },
        onError: (err) => {
          setSessions(prev => prev.map(s => {
            if (s.id !== session!.id) return s;
            const msgs = s.messages.map(m => {
              if (m.id === botMsgId) {
                return { ...m, text: err, isError: true };
              }
              return m;
            });
            return { ...s, messages: msgs };
          }));
          setIsProcessing(false);
        },
        onComplete: () => {
          setIsProcessing(false);
        }
      }
    );
  };

  const deleteSession = (e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    setSessions(prev => prev.filter(s => s.id !== id));
    if (currentSessionId === id) {
      setCurrentSessionId(null);
    }
  };

  return (
    <div className="flex h-screen bg-white dark:bg-dark-bg text-gray-900 dark:text-gray-100 font-sans overflow-hidden">
      
      {/* Sidebar */}
      <Sidebar 
        isOpen={isSidebarOpen} 
        onClose={() => setIsSidebarOpen(false)}
        sessions={sessions}
        currentSessionId={currentSessionId}
        onSelectSession={setCurrentSessionId}
        onNewChat={createNewSession}
        onDeleteSession={deleteSession}
        currentModel={currentModel}
        onOpenModelSelector={() => setIsModelSelectorOpen(true)}
      />

      <ModelSelector 
        isOpen={isModelSelectorOpen}
        onClose={() => setIsModelSelectorOpen(false)}
        models={modelConfigs}
        currentModelId={currentModelId}
        onSelectModel={setCurrentModelId}
        onUpdateModelKey={handleUpdateModelKey}
      />

      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col h-full relative">
        
        {/* Header */}
        <header className="h-14 border-b border-gray-100 dark:border-gray-800 flex items-center px-4 justify-between bg-white/80 dark:bg-dark-bg/80 backdrop-blur-md z-10 sticky top-0">
          <div className="flex items-center gap-3">
            <button 
              onClick={() => setIsSidebarOpen(!isSidebarOpen)}
              className="p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
            >
              <Menu size={20} className="text-gray-600 dark:text-gray-300" />
            </button>
            <div 
                className="flex flex-col cursor-pointer"
                onClick={() => setIsModelSelectorOpen(true)}
            >
              <span className="font-semibold text-sm md:text-base">{currentSession?.title || '小周AI库'}</span>
              <div className="flex items-center gap-1">
                 <span className="text-[10px] text-gray-400">{currentModel?.name}</span>
                 <div className={`w-1.5 h-1.5 rounded-full ${currentModel.userApiKey ? 'bg-green-400' : 'bg-gray-300'}`}></div>
              </div>
            </div>
          </div>
        </header>

        {/* Messages */}
        {currentSession && currentSession.messages.length > 0 ? (
          <MessageList messages={currentSession.messages} isThinking={isProcessing} />
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center text-center p-8 opacity-0 animate-[fadeIn_0.5s_ease-out_forwards]">
            <div className="w-20 h-20 bg-ds-500 rounded-2xl flex items-center justify-center mb-6 shadow-lg shadow-ds-500/30">
              <Sparkles className="text-white w-10 h-10" />
            </div>
            <h1 className="text-2xl font-bold mb-2">我是小周AI库</h1>
            <p className="text-gray-500 dark:text-gray-400 max-w-md mb-6">
              已启用 {currentModel.name}。我可以协助你进行创作、编码以及联网查询实时信息。
            </p>
            <button 
                onClick={() => setIsModelSelectorOpen(true)}
                className="text-sm text-ds-500 hover:text-ds-600 underline"
            >
                切换或配置模型 API Key
            </button>
          </div>
        )}

        {/* Input Area */}
        <div className="p-4 bg-white dark:bg-dark-bg border-t border-gray-100 dark:border-gray-800">
          <div className="max-w-3xl mx-auto relative">
            <div className="relative flex items-end gap-2 bg-gray-50 dark:bg-dark-surface border border-gray-200 dark:border-gray-700 rounded-2xl p-2 shadow-sm focus-within:ring-2 focus-within:ring-ds-500/50 transition-all">
              
              {/* Search Toggle */}
              <button 
                onClick={() => setEnableSearch(!enableSearch)}
                className={`p-2 rounded-xl flex-shrink-0 transition-colors ${
                  enableSearch 
                    ? 'bg-blue-100 text-blue-600 dark:bg-blue-900/40 dark:text-blue-400' 
                    : 'hover:bg-gray-200 dark:hover:bg-gray-600 text-gray-400'
                }`}
                title={enableSearch ? "已开启联网搜索" : "点击开启联网搜索"}
              >
                <Globe size={20} />
              </button>

              <textarea
                ref={textareaRef}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handleSendMessage();
                  }
                }}
                placeholder="问点什么..."
                className="flex-1 bg-transparent border-0 focus:ring-0 resize-none max-h-[150px] py-2 text-base"
                rows={1}
                disabled={isProcessing}
              />
              
              <button
                onClick={handleSendMessage}
                disabled={!input.trim() || isProcessing}
                className={`p-2 rounded-xl flex-shrink-0 transition-all ${
                  input.trim() && !isProcessing
                    ? 'bg-ds-500 text-white shadow-md hover:bg-ds-600'
                    : 'bg-gray-200 dark:bg-gray-700 text-gray-400 cursor-not-allowed'
                }`}
              >
                <Send size={20} />
              </button>
            </div>
            
            <div className="text-center text-xs text-gray-400 mt-2 flex justify-center gap-2">
              <span>AI生成内容仅供参考</span>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
};

export default App;