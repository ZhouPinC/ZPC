import { GoogleGenAI, GenerateContentResponse } from "@google/genai";
import { Message, Role, ModelConfig } from "../types";

interface StreamCallbacks {
  onChunk: (text: string) => void;
  onGrounding: (sources: {uri: string, title: string}[]) => void;
  onError: (error: string) => void;
  onComplete: () => void;
}

export const streamChatResponse = async (
  messages: Message[],
  modelConfig: ModelConfig,
  enableSearch: boolean,
  callbacks: StreamCallbacks
) => {
  try {
    // FIX: Use process.env.API_KEY exclusively.
    const ai = new GoogleGenAI({ apiKey: process.env.API_KEY });

    // 2. Prepare history
    const history = messages
      .filter(m => !m.isError && m.text.trim() !== '')
      .map(m => ({
        role: m.role === Role.USER ? 'user' : 'model',
        parts: [{ text: m.text }],
      }));

    const lastMessage = history.pop();
    if (!lastMessage || !lastMessage.parts[0].text) {
        throw new Error("No message to send.");
    }
    const prompt = lastMessage.parts[0].text;

    // 3. Configure Tools
    const tools: any[] = [];
    if (enableSearch && modelConfig.supportsSearch) {
      tools.push({ googleSearch: {} });
    }

    // 4. Configure Thinking
    let thinkingConfig = undefined;
    if (modelConfig.supportsThinking) {
        thinkingConfig = { thinkingBudget: 4096 }; 
    }

    // 5. System Instructions
    const baseInstruction = "You are a helpful AI assistant called '小周AI'. You are helpful, harmless, and honest. If you use search, summarize the results clearly.";
    const specificInstruction = modelConfig.systemInstruction || "";
    const finalInstruction = `${baseInstruction} ${specificInstruction}`;

    // 6. Create Chat
    const chat = ai.chats.create({
      model: modelConfig.geminiModelName,
      history: history,
      config: {
        tools: tools.length > 0 ? tools : undefined,
        thinkingConfig: thinkingConfig,
        systemInstruction: finalInstruction,
      }
    });

    // 7. Send Message Stream
    const resultStream = await chat.sendMessageStream({
      message: prompt
    });

    for await (const chunk of resultStream) {
      const response = chunk as GenerateContentResponse;
      
      if (response.text) {
        callbacks.onChunk(response.text);
      }

      const groundingChunks = response.candidates?.[0]?.groundingMetadata?.groundingChunks;
      if (groundingChunks) {
        const sources = groundingChunks
          .map((c: any) => c.web)
          .filter((w: any) => w && w.uri && w.title);
        if (sources.length > 0) {
            callbacks.onGrounding(sources);
        }
      }
    }

    callbacks.onComplete();

  } catch (error: any) {
    console.error("Gemini API Error:", error);
    let errorReason = "未知错误";
    let solution = "请稍后重试。";

    // Detailed Error Analysis
    if (error.message) {
        if (error.message.includes("400")) {
            errorReason = "请求格式无效";
            solution = "请检查您的输入内容是否包含违规信息。";
        } else if (error.message.includes("401") || error.message.includes("API key not valid")) {
            errorReason = "API Key 无效";
            solution = "请检查环境变量配置的 API Key。";
        } else if (error.message.includes("403")) {
            errorReason = "访问被拒绝";
            solution = "您的 API Key 可能没有权限访问该模型，或者服务所在的地区受限。";
        } else if (error.message.includes("429")) {
            errorReason = "请求过于频繁";
            solution = "您触发了 API 调用频率限制，请休息一下，稍后再试。";
        } else if (error.message.includes("500") || error.message.includes("503")) {
            errorReason = "AI 服务端异常";
            solution = "Google Gemini 服务暂时不可用，请稍后重试。";
        } else if (error.message.includes("fetch failed")) {
            errorReason = "网络连接失败";
            solution = "请检查您的网络连接，确能够访问 Google API 服务（可能需要科学上网）。";
        } else {
            errorReason = error.message.substring(0, 50) + "...";
        }
    }

    const friendlyMessage = `
**AI 无法回答**

**原因**: ${errorReason}

**建议**: ${solution}
    `.trim();

    callbacks.onError(friendlyMessage);
  }
};