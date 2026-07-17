import React, { useState, useEffect } from 'react';
import { ModelConfig, DEFAULT_API_KEY } from '../types';
import { Search, X, Check, Key, ChevronRight, Cpu } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

interface ModelSelectorProps {
  isOpen: boolean;
  onClose: () => void;
  models: ModelConfig[];
  currentModelId: string;
  onSelectModel: (modelId: string) => void;
  onUpdateModelKey: (modelId: string, key: string) => void;
}

const ModelSelector: React.FC<ModelSelectorProps> = ({
  isOpen,
  onClose,
  models,
  currentModelId,
  onSelectModel,
  onUpdateModelKey
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedForConfig, setSelectedForConfig] = useState<string | null>(null);
  const [tempKey, setTempKey] = useState('');

  // Reset state when opening
  useEffect(() => {
    if (isOpen) {
      setSearchTerm('');
      setSelectedForConfig(null);
    }
  }, [isOpen]);

  const filteredModels = models.filter(m => 
    m.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    m.provider.toLowerCase().includes(searchTerm.toLowerCase()) ||
    m.tags.some(t => t.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const handleConfigClick = (e: React.MouseEvent, model: ModelConfig) => {
    e.stopPropagation();
    setSelectedForConfig(model.id);
    setTempKey(model.userApiKey || '');
  };

  const saveKey = () => {
    if (selectedForConfig) {
      onUpdateModelKey(selectedForConfig, tempKey);
      setSelectedForConfig(null);
    }
  };

  const selectAndClose = (id: string) => {
    onSelectModel(id);
    onClose();
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4">
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            onClick={onClose}
          />
          
          <motion.div 
            initial={{ scale: 0.95, opacity: 0, y: 20 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            exit={{ scale: 0.95, opacity: 0, y: 20 }}
            transition={{ type: "spring", duration: 0.5 }}
            className="bg-white dark:bg-[#1e1e1e] w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden relative z-10 flex flex-col max-h-[80vh]"
          >
            {/* Header */}
            <div className="p-4 border-b border-gray-100 dark:border-gray-800 flex items-center gap-3">
              <div className="bg-ds-50 dark:bg-ds-900/50 p-2 rounded-lg text-ds-500">
                <Cpu size={20} />
              </div>
              <div className="flex-1">
                <h2 className="text-lg font-bold text-gray-800 dark:text-gray-100">选择 AI 模型</h2>
                <p className="text-xs text-gray-500">支持 Gemini Pro 2.5 驱动的多种预设</p>
              </div>
              <button onClick={onClose} className="p-2 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-full text-gray-500">
                <X size={20} />
              </button>
            </div>

            {/* Config Mode - API Key Input */}
            <AnimatePresence mode='wait'>
            {selectedForConfig ? (
               <motion.div 
                key="config"
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                className="flex-1 p-6 flex flex-col"
               >
                 <div className="flex items-center gap-2 mb-6">
                    <button onClick={() => setSelectedForConfig(null)} className="text-gray-400 hover:text-gray-600">
                       <ChevronRight size={20} className="rotate-180" />
                    </button>
                    <h3 className="font-semibold text-gray-700 dark:text-gray-200">
                      配置 {models.find(m => m.id === selectedForConfig)?.name}
                    </h3>
                 </div>

                 <div className="flex-1">
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                       API Key (必填)
                    </label>
                    <p className="text-xs text-gray-500 mb-3">
                      请输入该模型对应的有效 API Key。如果不填写，将尝试使用系统默认 Key (可能失效)。
                    </p>
                    <div className="relative">
                      <Key size={16} className="absolute left-3 top-3.5 text-gray-400" />
                      <input 
                        type="password"
                        value={tempKey}
                        onChange={(e) => setTempKey(e.target.value)}
                        placeholder={DEFAULT_API_KEY}
                        className="w-full pl-10 pr-4 py-3 bg-gray-50 dark:bg-dark-surface border border-gray-200 dark:border-gray-700 rounded-xl focus:ring-2 focus:ring-ds-500 outline-none transition-all font-mono text-sm"
                      />
                    </div>
                 </div>

                 <button 
                  onClick={saveKey}
                  className="mt-6 w-full bg-ds-500 text-white py-3 rounded-xl font-medium hover:bg-ds-600 transition-colors shadow-lg shadow-ds-500/30"
                 >
                   保存并连接
                 </button>
               </motion.div>
            ) : (
              <motion.div 
                key="list"
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                className="flex-1 flex flex-col overflow-hidden"
              >
                {/* Search */}
                <div className="px-4 py-3 bg-white dark:bg-[#1e1e1e]">
                  <div className="relative">
                    <Search size={16} className="absolute left-3 top-3 text-gray-400" />
                    <input 
                      type="text" 
                      placeholder="搜索模型 (例如: DeepSeek, 写作)..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="w-full bg-gray-100 dark:bg-dark-surface border-none rounded-xl py-2.5 pl-10 pr-4 text-sm focus:ring-2 focus:ring-ds-500/50 outline-none transition-all"
                    />
                  </div>
                </div>

                {/* List */}
                <div className="flex-1 overflow-y-auto p-2 space-y-1">
                  {filteredModels.map((model) => (
                    <div 
                      key={model.id}
                      onClick={() => selectAndClose(model.id)}
                      className={`group flex items-center justify-between p-3 rounded-xl cursor-pointer transition-all border ${
                        currentModelId === model.id
                          ? 'bg-ds-50 dark:bg-ds-900/20 border-ds-200 dark:border-ds-800 shadow-sm'
                          : 'border-transparent hover:bg-gray-50 dark:hover:bg-gray-800'
                      }`}
                    >
                      <div className="flex flex-col gap-0.5 max-w-[70%]">
                         <div className="flex items-center gap-2">
                           <span className={`font-semibold text-sm ${currentModelId === model.id ? 'text-ds-600 dark:text-ds-400' : 'text-gray-800 dark:text-gray-200'}`}>
                             {model.name}
                           </span>
                           {currentModelId === model.id && <Check size={14} className="text-ds-500" />}
                         </div>
                         <div className="text-xs text-gray-400 dark:text-gray-500">{model.provider}</div>
                         <div className="flex gap-1 mt-1">
                           {model.tags.slice(0, 3).map(tag => (
                             <span key={tag} className="text-[10px] bg-gray-100 dark:bg-gray-700 text-gray-500 px-1.5 py-0.5 rounded">
                               {tag}
                             </span>
                           ))}
                         </div>
                      </div>

                      <div className="flex items-center gap-2">
                        <button 
                          onClick={(e) => handleConfigClick(e, model)}
                          className="p-2 text-gray-400 hover:text-ds-500 hover:bg-ds-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                          title="配置 API Key"
                        >
                          <Key size={16} className={model.userApiKey ? "text-ds-500 fill-ds-500" : ""} />
                        </button>
                      </div>
                    </div>
                  ))}
                  
                  {filteredModels.length === 0 && (
                    <div className="text-center py-10 text-gray-400 text-sm">
                      没有找到匹配的模型
                    </div>
                  )}
                </div>
              </motion.div>
            )}
            </AnimatePresence>
            
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
};

export default ModelSelector;