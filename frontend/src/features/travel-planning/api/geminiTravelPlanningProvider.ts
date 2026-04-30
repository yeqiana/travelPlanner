import { GoogleGenAI, Type } from "@google/genai";
import { TravelPreferences, Itinerary, ChatMessage } from '../../../shared/types/travel';

const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });

export async function generateItineraryWithGemini(
  input: TravelPreferences | string,
  history: ChatMessage[] = []
): Promise<Itinerary> {
  const historyText = history
    .filter(m => !m.isLoading && m.text)
    .map(m => `${m.role === 'user' ? '用户' : 'AI'}: ${m.text}`).join('\n');

  let prompt = '';
  if (typeof input === 'string') {
    prompt = `
      根据历史对话，用户的最新要求是: "${input}"
      请结合之前的行程规划，给出更新后的完整行程。
      历史对话:
      ${historyText}
      返回严格的JSON格式数据。
    `;
  } else {
    prompt = `
      请作为一名专业的资深旅游规划师，为我生成一份详细的旅游行程。
      目的地: ${input.destinations}
      天数: ${input.days}天
      旅行风格/偏好: ${input.vibe}
      出行人员: ${input.companions}
      ${input.additionalNotes ? `补充说明/特殊要求: ${input.additionalNotes}` : ''}
      
      请确保行程安排合理，不要过于紧凑，考虑交通时间。
      为每一天规划一个主题（例如：“历史文化徒步”、“自然风光放松”等）。
      返回严格的JSON格式数据。
    `;
  }

  const response = await ai.models.generateContent({
    model: "gemini-3-flash-preview",
    contents: prompt,
    config: {
      responseMimeType: "application/json",
      responseSchema: {
        type: Type.OBJECT,
        properties: {
          assistantReply: { type: Type.STRING, description: "AI口语化回复，例如：'好的，没问题！我已为您将行程调整为...' 或者 '这是为您精心定制的杭州三日游，请查看～'" },
          title: { type: Type.STRING, description: "行程标题，例如：杭州三日深度文化游" },
          summary: { type: Type.STRING, description: "行程的一段简短概述，吸引人且突出亮点" },
          days: {
            type: Type.ARRAY,
            description: "每天的行程安排",
            items: {
              type: Type.OBJECT,
              properties: {
                dayNumber: { type: Type.INTEGER },
                theme: { type: Type.STRING, description: "当天的行程主题" },
                activities: {
                  type: Type.ARRAY,
                  items: {
                    type: Type.OBJECT,
                    properties: {
                      time: { type: Type.STRING, description: "活动开始时间，如 09:00" },
                      location: { type: Type.STRING, description: "地点或景点名称" },
                      description: { type: Type.STRING, description: "活动内容的详细描述" },
                      duration: { type: Type.STRING, description: "预计游玩时间，如 2小时" },
                      transportationToNext: { type: Type.STRING, description: "前往下一个地点的交通方式及预计时间（如果不适用可留空）" },
                      priceEstimate: {
                        type: Type.OBJECT,
                        description: "该地点附近的酒店和餐饮大致价格范围（仅作参考，填数字即可，货币默认人民币）",
                        properties: {
                          hotelMin: { type: Type.INTEGER },
                          hotelMax: { type: Type.INTEGER },
                          restaurantMin: { type: Type.INTEGER },
                          restaurantMax: { type: Type.INTEGER }
                        },
                        required: ["hotelMin", "hotelMax", "restaurantMin", "restaurantMax"]
                      }
                    },
                    required: ["time", "location", "description", "duration"]
                  }
                }
              },
              required: ["dayNumber", "theme", "activities"]
            }
          },
          tips: {
            type: Type.ARRAY,
            items: { type: Type.STRING },
            description: "实用的旅行贴士（如穿衣指南、避坑指南、餐饮建议等）至少3条"
          }
        },
        required: ["assistantReply", "title", "summary", "days", "tips"]
      }
    }
  });

  const jsonStr = response.text?.trim() || "{}";
  try {
    return JSON.parse(jsonStr) as Itinerary;
  } catch (error) {
    console.error("Failed to parse Gemini response:", jsonStr);
    throw new Error("生成行程失败，请重试。");
  }
}
