import React from 'react';
import { ChatSession, ModelConfig } from '../types';
import { Plus, MessageSquare, Trash2, X, ChevronRight, Settings2 } from 'lucide-react';
import { motion } from 'framer-motion';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  sessions: ChatSession[];
  currentSessionId: string | null;
  onSelectSession: (id: string) => void;
  onNewChat: () => void;
  onDeleteSession: (e: React.MouseEvent, id: string) => void;
  currentModel: ModelConfig;
  onOpenModelSelector: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({
  isOpen,
  onClose,
  sessions,
  currentSessionId,
  onSelectSession,
  onNewChat,
  onDeleteSession,
  currentModel,
  onOpenModelSelector
}) => {
  return (
    <>
      {/* Overlay for mobile */}
      {isOpen && (
        <div 
          className="fixed inset-0 bg-black/50 z-40 md:hidden"
          onClick={onClose}
        />
      )}

      {/* Sidebar Container */}
      <motion.div
        initial={false}
        animate={{ x: isOpen ? 0 : '-100%' }}
        transition={{ type: "spring", stiffness: 300, damping: 30 }}
        className={`fixed md:relative top-0 left-0 h-full w-[280px] bg-gray-50 dark:bg-[#1a1a1a] border-r border-gray-200 dark:border-gray-800 z-50 flex flex-col`}
      >
        {/* Header */}
        <div className="p-4 flex items-center justify-between">
           <h2 className="text-xl font-bold text-gray-800 dark:text-white flex items-center gap-2">
             <div className="w-6 h-6 bg-ds-500 rounded-md"></div>
             小周AI库
           </h2>
           <button onClick={onClose} className="md:hidden p-1 rounded hover:bg-gray-200 dark:hover:bg-gray-700 text-gray-600 dark:text-gray-300">
             <X size={20} />
           </button>
        </div>

        {/* New Chat Button */}
        <div className="px-4 pb-2">
          <button
            onClick={() => {
              onNewChat();
              if (window.innerWidth < 768) onClose();
            }}
            className="w-full flex items-center justify-center gap-2 bg-white dark:bg-dark-surface border border-gray-200 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-200 py-2.5 rounded-xl font-medium transition-all shadow-sm"
          >
            <Plus size={18} />
            开启新对话
          </button>
        </div>

        {/* Model Trigger */}
        <div className="px-4 py-2">
          <button 
            onClick={onOpenModelSelector}
            className="w-full flex items-center justify-between p-3 bg-ds-50 dark:bg-ds-900/10 border border-ds-100 dark:border-ds-800 rounded-xl group transition-all hover:border-ds-300"
          >
             <div className="flex flex-col items-start">
               <span className="text-[10px] text-gray-400 font-semibold uppercase tracking-wider">当前模型</span>
               <span className="font-semibold text-sm text-ds-700 dark:text-ds-400">{currentModel.name}</span>
             </div>
             <div className="p-1.5 bg-white dark:bg-dark-bg rounded-lg text-gray-400 group-hover:text-ds-500 transition-colors shadow-sm">
                <Settings2 size={16} />
             </div>
          </button>
        </div>

        {/* History List */}
        <div className="flex-1 overflow-y-auto px-2 mt-2">
          <div className="text-[10px] font-semibold text-gray-400 px-4 py-2 uppercase">最近记录</div>
          <div className="space-y-1">
            {sessions.length === 0 ? (
               <div className="text-center text-gray-400 py-10 text-sm">暂无历史记录</div>
            ) : (
              sessions.sort((a,b) => b.updatedAt - a.updatedAt).map(session => (
                <div
                  key={session.id}
                  onClick={() => {
                    onSelectSession(session.id);
                    if (window.innerWidth < 768) onClose();
                  }}
                  className={`group flex items-center justify-between p-3 rounded-lg cursor-pointer transition-colors ${
                    currentSessionId === session.id 
                      ? 'bg-gray-200 dark:bg-gray-800' 
                      : 'hover:bg-gray-100 dark:hover:bg-gray-800/50'
                  }`}
                >
                  <div className="flex items-center gap-3 overflow-hidden">
                    <MessageSquare size={16} className="text-gray-500 dark:text-gray-400 flex-shrink-0" />
                    <span className="text-sm text-gray-700 dark:text-gray-200 truncate">
                      {session.title || '新对话'}
                    </span>
                  </div>
                  <button
                    onClick={(e) => onDeleteSession(e, session.id)}
                    className="opacity-0 group-hover:opacity-100 p-1.5 rounded-md hover:bg-red-100 text-gray-400 hover:text-red-500 transition-all"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
        
        {/* Footer info */}
        <div className="p-4 text-xs text-center text-gray-400 border-t border-gray-200 dark:border-gray-800 flex justify-between items-center">
           <span>v1.2.0</span>
           <span>Power by Google</span>
        </div>

      </motion.div>
    </>
  );
};

export default Sidebar;