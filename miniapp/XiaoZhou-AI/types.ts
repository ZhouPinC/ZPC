export enum Role {
  USER = 'user',
  MODEL = 'model',
  SYSTEM = 'system'
}

export interface GroundingSource {
  uri: string;
  title: string;
}

export interface Message {
  id: string;
  role: Role;
  text: string;
  timestamp: number;
  isError?: boolean;
  groundingSources?: GroundingSource[];
  thinkingLog?: string; // Simulating "DeepThink" process
}

export interface ChatSession {
  id: string;
  title: string;
  messages: Message[];
  updatedAt: number;
  modelId: string;
}

export interface ModelConfig {
  id: string;
  name: string;
  provider: string;
  description: string;
  supportsSearch: boolean;
  supportsThinking: boolean;
  geminiModelName: string; // The underlying technical model name
  systemInstruction?: string; // Specific instruction to mimic other personas
  userApiKey?: string; // User provided key
  tags: string[];
}

export const DEFAULT_API_KEY = 'AIzaSyAFAT4j4sbLIDjPjXQ5bHTBz4hax4hrb4';

export const AVAILABLE_MODELS: ModelConfig[] = [
  {
    id: 'deepseek-r1-sim',
    name: 'DeepSeek R1 (DeepThink)',
    provider: 'DeepSeek (Simulated)',
    description: '深度思考 | 逻辑推理能力增强',
    supportsSearch: true,
    supportsThinking: true,
    tags: ['reasoning', 'coding', 'complex'],
    geminiModelName: 'gemini-3-pro-preview',
    systemInstruction: "You are a simulated version of DeepSeek R1. You are highly intelligent, logical, and you 'think' deeply before answering complex questions. When you explain things, be extremely detailed and structured."
  },
  {
    id: 'gemini-v2-5',
    name: 'Gemini 2.5 Pro',
    provider: 'Google',
    description: '综合能力最强 | 官方旗舰',
    supportsSearch: true,
    supportsThinking: false,
    tags: ['general', 'official', 'balanced'],
    geminiModelName: 'gemini-3-pro-preview' 
  },
  {
    id: 'gemini-flash',
    name: 'Gemini 2.5 Flash',
    provider: 'Google',
    description: '极速响应 | 低延迟',
    supportsSearch: true,
    supportsThinking: false,
    tags: ['fast', 'chat'],
    geminiModelName: 'gemini-2.5-flash'
  },
  {
    id: 'kimi-sim',
    name: 'Kimi AI (Simulated)',
    provider: 'Moonshot (Simulated)',
    description: '擅长长文阅读与中文写作',
    supportsSearch: true,
    supportsThinking: false,
    tags: ['writing', 'chinese'],
    geminiModelName: 'gemini-3-pro-preview',
    systemInstruction: "You are a helpful AI assistant simulating the style of Kimi AI. You excel at reading long contexts and writing fluent, natural Chinese. Your tone is polite, professional, and warm."
  },
  {
    id: 'tongyi-sim',
    name: 'Qwen Max (Simulated)',
    provider: 'Alibaba (Simulated)',
    description: '通义千问 | 知识广博',
    supportsSearch: true,
    supportsThinking: false,
    tags: ['knowledge', 'creative'],
    geminiModelName: 'gemini-3-pro-preview',
    systemInstruction: "You are simulating Alibaba's Qwen model. You are knowledgeable about Chinese culture, history, and modern trends."
  }
];