import React, { useEffect, useRef } from 'react';
import { Message, Role } from '../types';
import MarkdownRenderer from './MarkdownRenderer';
import { motion, AnimatePresence } from 'framer-motion';
import { User, Bot, Globe, AlertCircle } from 'lucide-react';

interface MessageListProps {
  messages: Message[];
  isThinking: boolean;
}

const MessageList: React.FC<MessageListProps> = ({ messages, isThinking }) => {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isThinking]);

  return (
    <div className="flex-1 overflow-y-auto p-4 space-y-6 scroll-smooth">
      <AnimatePresence initial={false}>
        {messages.map((msg) => (
          <motion.div
            key={msg.id}
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, ease: "easeOut" }}
            className={`flex w-full ${msg.role === Role.USER ? 'justify-end' : 'justify-start'}`}
          >
            <div className={`flex max-w-[90%] md:max-w-[80%] gap-3 ${msg.role === Role.USER ? 'flex-row-reverse' : 'flex-row'}`}>
              
              {/* Avatar */}
              <div className={`flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center ${
                msg.role === Role.USER ? 'bg-gray-200 dark:bg-gray-700' : 'bg-ds-500 text-white'
              }`}>
                {msg.role === Role.USER ? <User size={18} className="text-gray-600 dark:text-gray-300" /> : <Bot size={18} />}
              </div>

              {/* Bubble */}
              <div className={`flex flex-col ${msg.role === Role.USER ? 'items-end' : 'items-start'}`}>
                <div className={`rounded-2xl px-4 py-3 shadow-sm ${
                  msg.role === Role.USER 
                    ? 'bg-ds-500 text-white rounded-tr-none' 
                    : 'bg-white dark:bg-dark-surface text-gray-800 dark:text-gray-100 rounded-tl-none border border-gray-100 dark:border-gray-700'
                }`}>
                   {msg.isError ? (
                     <div className="flex items-center gap-2 text-red-500 dark:text-red-400">
                        <AlertCircle size={16} />
                        <span className="text-sm">{msg.text}</span>
                     </div>
                   ) : (
                     <MarkdownRenderer content={msg.text} />
                   )}
                </div>

                {/* Grounding Sources */}
                {msg.groundingSources && msg.groundingSources.length > 0 && (
                  <div className="mt-2 flex flex-wrap gap-2">
                    {msg.groundingSources.map((source, idx) => (
                      <a 
                        key={idx} 
                        href={source.uri} 
                        target="_blank" 
                        rel="noreferrer"
                        className="flex items-center gap-1 text-xs bg-gray-100 dark:bg-gray-800 text-blue-600 dark:text-blue-400 px-2 py-1 rounded-full hover:bg-blue-50 dark:hover:bg-gray-700 transition-colors"
                      >
                        <Globe size={10} />
                        <span className="truncate max-w-[150px]">{source.title}</span>
                      </a>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </motion.div>
        ))}
      </AnimatePresence>
      
      {isThinking && (
        <motion.div 
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="flex justify-start w-full"
        >
          <div className="flex gap-3 max-w-[80%]">
             <div className="flex-shrink-0 w-8 h-8 rounded-full bg-ds-500 flex items-center justify-center text-white">
                <Bot size={18} />
             </div>
             <div className="bg-white dark:bg-dark-surface rounded-2xl rounded-tl-none px-4 py-3 border border-gray-100 dark:border-gray-700 flex items-center gap-2">
                <div className="w-2 h-2 bg-ds-500 rounded-full animate-bounce" style={{ animationDelay: '0s' }}></div>
                <div className="w-2 h-2 bg-ds-500 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                <div className="w-2 h-2 bg-ds-500 rounded-full animate-bounce" style={{ animationDelay: '0.4s' }}></div>
                <span className="text-xs text-gray-400 ml-1">思考中...</span>
             </div>
          </div>
        </motion.div>
      )}
      <div ref={bottomRef} />
    </div>
  );
};

export default MessageList;