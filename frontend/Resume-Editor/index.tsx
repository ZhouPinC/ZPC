import React, { useState, useEffect, useRef } from "react";
import { createRoot } from "react-dom/client";
import { 
  Phone, 
  Mail, 
  MapPin, 
  Briefcase, 
  GraduationCap, 
  Award, 
  Code, 
  User,
  Calendar,
  Download,
  Edit2,
  Check,
  Plus,
  Trash2,
  X,
  Upload,
  Layout,
  Palette,
  Github,
  Globe,
  FileJson,
  UploadCloud,
  Save,
  LogOut,
  ArrowUp,
  ArrowDown
} from "lucide-react";

// --- Types & Initial Data ---

interface Project {
  name: string;
  role: string;
  date: string;
  desc: string;
  details: string[];
}

interface Education {
  institution: string;
  major: string;
  degree: string;
  start: string;
  end: string;
}

interface Skills {
  languages: string[];
  frontend: string[];
  backend: string[];
  hardware: string[];
  others: string[];
  [key: string]: string[];
}

interface Basics {
  name: string;
  title: string;
  phone: string;
  email: string;
  location: string;
  salary: string;
  experience: string;
  summary: string;
  avatar: string;
  major: string;
}

interface ResumeData {
  basics: Basics;
  education: Education[];
  skills: Skills;
  certificates: string[];
  projects: Project[];
}

const THEME_COLORS = {
  blue: "blue",
  emerald: "emerald",
  violet: "violet",
  slate: "slate",
  rose: "rose"
};

const TEMPLATES = {
  modern: "modern",
  classic: "classic"
};

// Placeholder data for public repository
const initialResumeData: ResumeData = {
  basics: {
    name: "您的姓名",
    title: "求职意向 / 职位头衔",
    phone: "138-xxxx-xxxx",
    email: "example@email.com",
    location: "意向城市",
    salary: "期望薪资",
    experience: "年龄 | 经验",
    summary: "这里是您的个人简介。请简要描述您的专业背景、核心技能以及职业目标。例如：具有X年开发经验，熟悉React与Node.js，善于解决复杂问题，具备良好的团队协作能力，对新技术保持敏锐的洞察力。",
    avatar: "",
    major: "主修专业"
  },
  education: [
    {
      institution: "毕业院校名称",
      major: "主修专业",
      degree: "学历/学位",
      start: "20xx",
      end: "20xx"
    }
  ],
  skills: {
    languages: ["Java", "C++", "Python", "JavaScript", "TypeScript"],
    frontend: ["React", "Vue", "Android", "Flutter", "iOS"],
    backend: ["Node.js", "MySQL", "Redis", "Docker"],
    hardware: ["Arduino", "Raspberry Pi", "IoT Sensors"],
    others: ["Git", "Linux", "Agile", "CI/CD"]
  },
  certificates: [
    "证书名称示例 1 (例如：CET-6)",
    "证书名称示例 2 (例如：PMP认证)",
    "证书名称示例 3",
    "相关技能证书 A",
    "相关技能证书 B"
  ],
  projects: [
    {
      name: "项目名称示例 A",
      role: "负责人/开发者",
      date: "20xx.xx - 20xx.xx",
      desc: "简要描述项目的核心功能、目标以及您在其中发挥的作用。",
      details: [
        "负责的具体工作内容描述，使用了什么技术解决了什么问题。",
        "实现了某某核心模块，提升了系统性能xx%。",
        "优化了用户体验，主导了技术选型与架构设计。"
      ]
    },
    {
      name: "项目名称示例 B",
      role: "核心成员",
      date: "20xx.xx - 20xx.xx",
      desc: "另一个项目的简要描述。",
      details: [
        "参与了模块的需求分析与编码工作。",
        "配合团队完成了系统的测试与上线。",
        "编写了自动化脚本，提高了开发效率。"
      ]
    },
    {
      name: "个人开源项目",
      role: "独立开发",
      date: "20xx.xx",
      desc: "基于某某技术栈开发的个人兴趣项目。",
      details: [
        "项目亮点功能介绍一。",
        "项目亮点功能介绍二。"
      ]
    }
  ]
};

// --- Helper Components ---

const EditableField = ({ 
  value, 
  onChange, 
  isEditing, 
  className = "", 
  placeholder = "", 
  multiline = false,
  darkTheme = false 
}: any) => {
  if (!isEditing) {
    return <span className={`${className} whitespace-pre-wrap`}>{value}</span>;
  }

  const baseInputStyles = `w-full bg-opacity-20 transition-colors duration-200 outline-none p-1 rounded ${
    darkTheme 
      ? "bg-white/10 text-white border-b border-white/30 placeholder-white/30 focus:bg-white/20" 
      : "bg-black/5 text-slate-800 border-b border-black/20 placeholder-black/30 focus:bg-black/10"
  }`;

  if (multiline) {
    return (
      <textarea
        value={value || ""}
        onChange={(e) => onChange(e.target.value)}
        className={`${baseInputStyles} ${className} min-h-[4em] resize-y`}
        placeholder={placeholder}
      />
    );
  }

  return (
    <input
      type="text"
      value={value || ""}
      onChange={(e) => onChange(e.target.value)}
      className={`${baseInputStyles} ${className}`}
      placeholder={placeholder}
    />
  );
};

const SectionControls = ({ onAdd, label, isEditing, darkTheme = false }: any) => {
  if (!isEditing) return null;
  return (
    <button 
      onClick={onAdd}
      className={`mt-2 flex items-center gap-1 text-xs font-bold uppercase tracking-wider py-1.5 px-3 rounded border border-dashed transition-all opacity-60 hover:opacity-100 ${
        darkTheme 
          ? "border-white/40 text-white/80 hover:bg-white/10" 
          : "border-slate-400 text-slate-600 hover:bg-slate-100"
      }`}
    >
      <Plus size={14} /> 添加{label}
    </button>
  );
};

const DeleteButton = ({ onClick }: any) => (
  <button 
    onClick={onClick}
    className="p-1 text-red-400 hover:text-red-600 hover:bg-red-50 rounded transition-colors"
    title="删除"
  >
    <Trash2 size={14} />
  </button>
);

const MoveButton = ({ onClick, direction, disabled }: any) => (
  <button 
    onClick={onClick}
    disabled={disabled}
    className={`p-1 rounded transition-colors ${
      disabled 
        ? "text-gray-300 cursor-not-allowed" 
        : "text-slate-400 hover:text-slate-600 hover:bg-slate-100"
    }`}
    title={direction === 'up' ? "上移" : "下移"}
  >
    {direction === 'up' ? <ArrowUp size={14} /> : <ArrowDown size={14} />}
  </button>
);

// --- Resume Templates ---

const ModernTemplate = ({ data, setData, isEditing, themeColor, moveProject }: {
  data: ResumeData;
  setData: (data: ResumeData) => void;
  isEditing: boolean;
  themeColor: string;
  moveProject: (index: number, direction: 'up' | 'down') => void;
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleImageUpload = (e: any) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        if (typeof reader.result === 'string') {
          setData({ ...data, basics: { ...data.basics, avatar: reader.result } });
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const getThemeClass = (type: string) => {
    const colors: any = {
      blue: { bg: "bg-slate-900", text: "text-blue-400", border: "border-blue-500/30" },
      emerald: { bg: "bg-slate-900", text: "text-emerald-400", border: "border-emerald-500/30" },
      violet: { bg: "bg-slate-900", text: "text-violet-400", border: "border-violet-500/30" },
      slate: { bg: "bg-slate-900", text: "text-slate-300", border: "border-slate-500/30" },
      rose: { bg: "bg-slate-900", text: "text-rose-400", border: "border-rose-500/30" },
    };
    return colors[themeColor]?.[type] || colors.blue[type];
  };

  return (
    <div className="flex flex-col md:flex-row print:flex-row min-h-[1123px] w-full bg-white shadow-xl print:shadow-none print:w-full">
      {/* Sidebar - Dark Theme - Expanded width to 38% and increased padding/gaps */}
      <div className={`w-full md:w-[38%] print:w-[38%] ${getThemeClass('bg')} text-slate-300 p-8 flex flex-col gap-8`}>
        {/* Avatar Section */}
        <div className="flex flex-col items-center gap-4">
          <div className="relative group">
            {/* Increased avatar size to w-32 h-32 */}
            <div className="w-32 h-32 rounded-full overflow-hidden border-4 border-white/10 bg-white/5 flex items-center justify-center">
              {data.basics.avatar ? (
                <img src={data.basics.avatar} alt="Profile" className="w-full h-full object-cover" />
              ) : (
                <User size={48} className="text-white/20" />
              )}
            </div>
            {isEditing && (
              <button 
                onClick={() => fileInputRef.current?.click()}
                className="absolute inset-0 bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity rounded-full cursor-pointer"
              >
                <Upload size={24} className="text-white" />
              </button>
            )}
            <input type="file" ref={fileInputRef} className="hidden" accept="image/*" onChange={handleImageUpload} />
          </div>

          <div className="text-center w-full">
            <EditableField 
              value={data.basics.name} 
              onChange={(val: string) => setData({...data, basics: {...data.basics, name: val}})}
              isEditing={isEditing}
              darkTheme={true}
              className="text-2xl font-bold text-white block mb-0.5"
              placeholder="您的姓名"
            />
            <EditableField 
              value={data.basics.title} 
              onChange={(val: string) => setData({...data, basics: {...data.basics, title: val}})}
              isEditing={isEditing}
              darkTheme={true}
              className={`text-base block ${getThemeClass('text')}`}
              placeholder="职位头衔"
            />
            <EditableField 
              value={data.basics.major} 
              onChange={(val: string) => setData({...data, basics: {...data.basics, major: val}})}
              isEditing={isEditing}
              darkTheme={true}
              className="text-sm block opacity-80 mt-0.5"
              placeholder="主修专业"
            />
          </div>

          <div className="flex flex-col items-center gap-1 text-sm w-full opacity-90">
             <div className="flex items-center gap-2">
                <EditableField 
                  value={data.basics.experience} 
                  onChange={(val: string) => setData({...data, basics: {...data.basics, experience: val}})}
                  isEditing={isEditing}
                  darkTheme={true}
                  placeholder="年龄 | 经验"
                />
             </div>
             <div className="flex items-center gap-2">
                <EditableField 
                  value={data.basics.salary} 
                  onChange={(val: string) => setData({...data, basics: {...data.basics, salary: val}})}
                  isEditing={isEditing}
                  darkTheme={true}
                  placeholder="期望薪资"
                />
             </div>
          </div>
        </div>

        <div className="w-full h-px bg-white/10" />

        {/* Contact Info */}
        <div className="flex flex-col gap-4 text-sm">
          <div className="flex items-center gap-3">
            <Phone size={16} className={getThemeClass('text')} />
            <EditableField 
              value={data.basics.phone} 
              onChange={(val: string) => setData({...data, basics: {...data.basics, phone: val}})}
              isEditing={isEditing}
              darkTheme={true}
              placeholder="电话号码"
            />
          </div>
          <div className="flex items-center gap-3">
            <Mail size={16} className={getThemeClass('text')} />
            <EditableField 
              value={data.basics.email} 
              onChange={(val: string) => setData({...data, basics: {...data.basics, email: val}})}
              isEditing={isEditing}
              darkTheme={true}
              placeholder="电子邮箱"
            />
          </div>
          <div className="flex items-center gap-3">
            <MapPin size={16} className={getThemeClass('text')} />
            <EditableField 
              value={data.basics.location} 
              onChange={(val: string) => setData({...data, basics: {...data.basics, location: val}})}
              isEditing={isEditing}
              darkTheme={true}
              placeholder="所在城市"
            />
          </div>
        </div>

        <div className="w-full h-px bg-white/10" />

        {/* Education */}
        <div>
          <h3 className="flex items-center gap-2 text-lg font-bold text-white mb-4">
            <GraduationCap size={20} /> 教育经历
          </h3>
          <div className="flex flex-col gap-4">
            {data.education.map((edu, index) => (
              <div key={index} className="relative group">
                {isEditing && (
                  <div className="absolute -right-2 top-0">
                     <DeleteButton onClick={() => {
                        const newEdu = [...data.education];
                        newEdu.splice(index, 1);
                        setData({...data, education: newEdu});
                     }} />
                  </div>
                )}
                <EditableField 
                  value={edu.institution} 
                  onChange={(val: string) => {
                    const newEdu = [...data.education];
                    newEdu[index].institution = val;
                    setData({...data, education: newEdu});
                  }}
                  isEditing={isEditing}
                  darkTheme={true}
                  className="font-bold text-sm block text-white"
                  placeholder="学校名称"
                />
                <div className="flex justify-between text-xs mt-1 text-white/70">
                   <div className="flex gap-1.5 flex-wrap">
                      <EditableField 
                        value={edu.major} 
                        onChange={(val: string) => {
                          const newEdu = [...data.education];
                          newEdu[index].major = val;
                          setData({...data, education: newEdu});
                        }}
                        isEditing={isEditing}
                        darkTheme={true}
                        placeholder="专业"
                      />
                       <span>|</span>
                      <EditableField 
                        value={edu.degree} 
                        onChange={(val: string) => {
                          const newEdu = [...data.education];
                          newEdu[index].degree = val;
                          setData({...data, education: newEdu});
                        }}
                        isEditing={isEditing}
                        darkTheme={true}
                        placeholder="学位"
                      />
                   </div>
                </div>
                <div className="text-xs mt-1 opacity-60">
                   <span className="flex gap-1">
                    <EditableField 
                        value={edu.start} 
                        onChange={(val: string) => {
                          const newEdu = [...data.education];
                          newEdu[index].start = val;
                          setData({...data, education: newEdu});
                        }}
                        isEditing={isEditing}
                        darkTheme={true}
                        placeholder="开始年份"
                      />
                      -
                      <EditableField 
                        value={edu.end} 
                        onChange={(val: string) => {
                          const newEdu = [...data.education];
                          newEdu[index].end = val;
                          setData({...data, education: newEdu});
                        }}
                        isEditing={isEditing}
                        darkTheme={true}
                        placeholder="结束年份"
                      />
                   </span>
                </div>
              </div>
            ))}
            <SectionControls 
              isEditing={isEditing} 
              onAdd={() => setData({...data, education: [...data.education, { institution: "学校名称", major: "专业", degree: "学位", start: "20xx", end: "20xx" }]})} 
              label="教育经历"
              darkTheme={true}
            />
          </div>
        </div>

        <div className="w-full h-px bg-white/10" />

        {/* Skills */}
        <div>
          <h3 className="flex items-center gap-2 text-lg font-bold text-white mb-4">
            <Code size={20} /> 专业技能
          </h3>
          <div className="flex flex-col gap-4">
            {Object.entries(data.skills).map(([category, items]) => (
              <div key={category} className="mb-0.5">
                <h4 className={`text-xs font-bold mb-2 uppercase opacity-80 ${getThemeClass('text')}`}>
                  {category === 'languages' ? '开发语言' : 
                   category === 'frontend' ? '前端/移动端' : 
                   category === 'backend' ? '后端/数据库' :
                   category === 'hardware' ? '硬件/嵌入式' : '其他工具'}
                </h4>
                <div className="flex flex-wrap gap-2">
                  {items.map((skill, idx) => (
                    <span key={idx} className="relative group inline-block">
                      {/* Restored standard text size (text-xs) */}
                      <span className="bg-white/10 px-2 py-1 rounded text-xs text-slate-200 border border-white/10">
                        <EditableField 
                           value={skill}
                           onChange={(val: string) => {
                             const newSkills = {...data.skills};
                             newSkills[category][idx] = val;
                             setData({...data, skills: newSkills});
                           }}
                           isEditing={isEditing}
                           darkTheme={true}
                           className="text-xs"
                        />
                      </span>
                      {isEditing && (
                        <button 
                          onClick={() => {
                             const newSkills = {...data.skills};
                             newSkills[category] = items.filter((_, i) => i !== idx);
                             setData({...data, skills: newSkills});
                          }}
                          className="absolute -top-1 -right-1 bg-red-500 text-white rounded-full p-0.5 opacity-0 group-hover:opacity-100 transition-opacity"
                        >
                          <X size={8} />
                        </button>
                      )}
                    </span>
                  ))}
                  {isEditing && (
                    <button 
                      onClick={() => {
                        const newSkills = {...data.skills};
                        newSkills[category] = [...items, "New Skill"];
                        setData({...data, skills: newSkills});
                      }}
                      className="bg-white/5 px-2 py-1 rounded text-xs text-white/50 border border-dashed border-white/20 hover:text-white hover:border-white/50 transition-colors"
                    >
                      +
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="w-full h-px bg-white/10" />

         {/* Certificates */}
         <div>
          <h3 className="flex items-center gap-2 text-lg font-bold text-white mb-4">
            <Award size={20} /> 资格证书
          </h3>
          <ul className="flex flex-col gap-2">
            {data.certificates.map((cert, index) => (
              <li key={index} className="text-sm text-slate-300 relative group flex items-start gap-2">
                 <span className={`mt-1.5 w-1.5 h-1.5 rounded-full ${getThemeClass('bg').replace('bg-', 'bg-') === 'bg-slate-900' ? 'bg-white/40' : 'bg-white/40'}`}></span>
                 <EditableField 
                    value={cert}
                    onChange={(val: string) => {
                       const newCerts = [...data.certificates];
                       newCerts[index] = val;
                       setData({...data, certificates: newCerts});
                    }}
                    isEditing={isEditing}
                    darkTheme={true}
                 />
                 {isEditing && (
                   <div className="ml-auto">
                    <DeleteButton onClick={() => {
                       const newCerts = [...data.certificates];
                       newCerts.splice(index, 1);
                       setData({...data, certificates: newCerts});
                    }} />
                   </div>
                 )}
              </li>
            ))}
            <SectionControls 
              isEditing={isEditing} 
              onAdd={() => setData({...data, certificates: [...data.certificates, "新证书"]})} 
              label="证书"
              darkTheme={true}
            />
          </ul>
        </div>
      </div>

      {/* Main Content - Adjusted width to 62% */}
      <div className="w-full md:w-[62%] print:w-[62%] p-10 text-slate-800">
        
        {/* Summary Section */}
        <section className="mb-10">
          <h2 className="flex items-center gap-3 text-2xl font-bold text-slate-800 mb-6 border-b-2 border-slate-100 pb-2">
            <User className={getThemeClass('text')} /> 个人优势
          </h2>
          <div className="text-sm leading-relaxed text-slate-600">
            <EditableField 
               value={data.basics.summary} 
               onChange={(val: string) => setData({...data, basics: {...data.basics, summary: val}})}
               isEditing={isEditing}
               multiline={true}
               className="w-full block"
               placeholder="个人简介..."
            />
          </div>
        </section>

        {/* Projects Section */}
        <section>
          <h2 className="flex items-center gap-3 text-2xl font-bold text-slate-800 mb-6 border-b-2 border-slate-100 pb-2">
            <Briefcase className={getThemeClass('text')} /> 项目经历
          </h2>
          
          <div className="flex flex-col space-y-5">
            {data.projects.map((project, index) => (
              <div key={index} className="relative group border-l-2 border-slate-100 pl-4 hover:border-slate-200 transition-colors">
                 {isEditing && (
                  <div className="absolute right-0 top-0 flex gap-1 bg-white p-1 rounded shadow-sm opacity-0 group-hover:opacity-100 transition-opacity z-10">
                     <MoveButton 
                        onClick={() => moveProject(index, 'up')} 
                        direction="up" 
                        disabled={index === 0} 
                     />
                     <MoveButton 
                        onClick={() => moveProject(index, 'down')} 
                        direction="down" 
                        disabled={index === data.projects.length - 1} 
                     />
                     <DeleteButton onClick={() => {
                        const newProjects = [...data.projects];
                        newProjects.splice(index, 1);
                        setData({...data, projects: newProjects});
                     }} />
                  </div>
                )}
                <div className="flex justify-between items-baseline mb-1">
                  <h3 className="text-xl font-bold text-slate-800">
                     <EditableField 
                        value={project.name}
                        onChange={(val: string) => {
                           const newProjects = [...data.projects];
                           newProjects[index].name = val;
                           setData({...data, projects: newProjects});
                        }}
                        isEditing={isEditing}
                        placeholder="项目名称"
                     />
                  </h3>
                  <div className="text-sm font-medium text-slate-500 font-mono">
                     <EditableField 
                        value={project.date}
                        onChange={(val: string) => {
                           const newProjects = [...data.projects];
                           newProjects[index].date = val;
                           setData({...data, projects: newProjects});
                        }}
                        isEditing={isEditing}
                        placeholder="时间"
                     />
                  </div>
                </div>
                
                <div className="mb-2">
                   <span className={`inline-block text-xs font-bold px-2 py-0.5 rounded ${getThemeClass('bg').replace('bg-slate-900', 'bg-slate-100')} ${getThemeClass('text')}`}>
                      <EditableField 
                        value={project.role}
                        onChange={(val: string) => {
                           const newProjects = [...data.projects];
                           newProjects[index].role = val;
                           setData({...data, projects: newProjects});
                        }}
                        isEditing={isEditing}
                        placeholder="担任角色"
                     />
                   </span>
                </div>

                <div className="text-sm text-slate-600 mb-2 italic">
                   <EditableField 
                        value={project.desc}
                        onChange={(val: string) => {
                           const newProjects = [...data.projects];
                           newProjects[index].desc = val;
                           setData({...data, projects: newProjects});
                        }}
                        isEditing={isEditing}
                        multiline={true}
                        placeholder="项目描述"
                     />
                </div>

                <ul className="text-sm text-slate-600 list-disc list-outside ml-4 space-y-1">
                  {(project.details || []).map((detail, dIdx) => (
                    <li key={dIdx} className="relative group">
                       <EditableField 
                        value={detail}
                        onChange={(val: string) => {
                           const newProjects = [...data.projects];
                           newProjects[index].details[dIdx] = val;
                           setData({...data, projects: newProjects});
                        }}
                        isEditing={isEditing}
                        multiline={true}
                        placeholder="项目详情"
                     />
                     {isEditing && (
                        <button 
                           onClick={() => {
                              const newProjects = [...data.projects];
                              newProjects[index].details = project.details.filter((_, i) => i !== dIdx);
                              setData({...data, projects: newProjects});
                           }}
                           className="absolute -left-5 top-1 opacity-0 group-hover:opacity-100 text-red-400 hover:text-red-600"
                        >
                           <X size={12} />
                        </button>
                     )}
                    </li>
                  ))}
                   <li className="list-none"> {/* Always show add button */}
                     {isEditing && (
                        <button 
                           onClick={() => {
                              const newProjects = [...data.projects];
                              if(!newProjects[index].details) newProjects[index].details = [];
                              newProjects[index].details.push("新项目详情");
                              setData({...data, projects: newProjects});
                           }}
                           className="text-xs text-slate-400 hover:text-slate-600 flex items-center gap-1 mt-1 -ml-4"
                        >
                           <Plus size={12} /> 添加详情
                        </button>
                     )}
                   </li>
                </ul>
              </div>
            ))}
            <SectionControls 
              isEditing={isEditing} 
              onAdd={() => setData({...data, projects: [...data.projects, {
                 name: "新项目",
                 role: "角色",
                 date: "20xx.xx",
                 desc: "项目简述",
                 details: ["项目详情"]
              }]})} 
              label="项目经历"
            />
          </div>
        </section>
      </div>
    </div>
  );
};

const ClassicTemplate = ({ data, setData, isEditing, themeColor, moveProject }: {
  data: ResumeData;
  setData: (data: ResumeData) => void;
  isEditing: boolean;
  themeColor: string;
  moveProject: (index: number, direction: 'up' | 'down') => void;
}) => {
   const fileInputRef = useRef<HTMLInputElement>(null);

   const handleImageUpload = (e: any) => {
     const file = e.target.files[0];
     if (file) {
       const reader = new FileReader();
       reader.onloadend = () => {
        if (typeof reader.result === 'string') {
          setData({ ...data, basics: { ...data.basics, avatar: reader.result } });
        }
       };
       reader.readAsDataURL(file);
     }
   };
 
   const getThemeClass = (type: string) => {
     const colors: any = {
       blue: { text: "text-blue-700", border: "border-blue-700", bg: "bg-blue-50" },
       emerald: { text: "text-emerald-700", border: "border-emerald-700", bg: "bg-emerald-50" },
       violet: { text: "text-violet-700", border: "border-violet-700", bg: "bg-violet-50" },
       slate: { text: "text-slate-700", border: "border-slate-700", bg: "bg-slate-50" },
       rose: { text: "text-rose-700", border: "border-rose-700", bg: "bg-rose-50" },
     };
     return colors[themeColor]?.[type] || colors.blue[type];
   };

   return (
      <div className="w-full min-h-[1123px] bg-white shadow-xl print:shadow-none p-12 text-slate-800 flex flex-col gap-6">
         {/* Header */}
         <div className={`flex items-start justify-between border-b-4 ${getThemeClass('border')} pb-6`}>
            <div className="flex-1">
               <h1 className="text-4xl font-bold mb-2 tracking-tight">
                  <EditableField 
                     value={data.basics.name}
                     onChange={(val: string) => setData({...data, basics: {...data.basics, name: val}})}
                     isEditing={isEditing}
                     placeholder="您的姓名"
                  />
               </h1>
               <div className={`text-xl ${getThemeClass('text')} font-medium mb-4`}>
                  <EditableField 
                     value={data.basics.title}
                     onChange={(val: string) => setData({...data, basics: {...data.basics, title: val}})}
                     isEditing={isEditing}
                     placeholder="职位头衔"
                  />
                  <div className="text-base mt-1 text-slate-500">
                     <EditableField 
                        value={data.basics.major} 
                        onChange={(val: string) => setData({...data, basics: {...data.basics, major: val}})}
                        isEditing={isEditing}
                        placeholder="主修专业"
                     />
                  </div>
               </div>
               <div className="flex flex-wrap gap-x-6 gap-y-2 text-sm text-slate-600">
                  <div className="flex items-center gap-1">
                     <Phone size={14} />
                     <EditableField 
                        value={data.basics.phone}
                        onChange={(val: string) => setData({...data, basics: {...data.basics, phone: val}})}
                        isEditing={isEditing}
                        placeholder="电话"
                     />
                  </div>
                  <div className="flex items-center gap-1">
                     <Mail size={14} />
                     <EditableField 
                        value={data.basics.email}
                        onChange={(val: string) => setData({...data, basics: {...data.basics, email: val}})}
                        isEditing={isEditing}
                        placeholder="邮箱"
                     />
                  </div>
                  <div className="flex items-center gap-1">
                     <MapPin size={14} />
                     <EditableField 
                        value={data.basics.location}
                        onChange={(val: string) => setData({...data, basics: {...data.basics, location: val}})}
                        isEditing={isEditing}
                        placeholder="地点"
                     />
                  </div>
                  <div className="flex items-center gap-1">
                     <User size={14} />
                     <EditableField 
                        value={data.basics.experience}
                        onChange={(val: string) => setData({...data, basics: {...data.basics, experience: val}})}
                        isEditing={isEditing}
                        placeholder="经验"
                     />
                  </div>
                  <div className="flex items-center gap-1">
                     <span className="font-bold">¥</span>
                     <EditableField 
                        value={data.basics.salary}
                        onChange={(val: string) => setData({...data, basics: {...data.basics, salary: val}})}
                        isEditing={isEditing}
                        placeholder="期望薪资"
                     />
                  </div>
               </div>
            </div>
            <div className="relative group w-24 h-24 shrink-0 ml-6">
                <div className="w-full h-full bg-slate-100 rounded overflow-hidden">
                   {data.basics.avatar ? (
                      <img src={data.basics.avatar} alt="Profile" className="w-full h-full object-cover" />
                   ) : (
                      <div className="w-full h-full flex items-center justify-center text-slate-300">
                         <User size={32} />
                      </div>
                   )}
                </div>
                {isEditing && (
                  <button 
                    onClick={() => fileInputRef.current?.click()}
                    className="absolute inset-0 bg-black/50 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity rounded cursor-pointer text-white"
                  >
                    <Upload size={16} />
                  </button>
                )}
                <input type="file" ref={fileInputRef} className="hidden" accept="image/*" onChange={handleImageUpload} />
            </div>
         </div>

         {/* Three Column Grid for Meta Info */}
         <div className="grid grid-cols-1 md:grid-cols-3 print:grid-cols-3 gap-6">
            
            {/* Education */}
            <div className="col-span-2">
               <h2 className={`text-lg font-bold uppercase tracking-wider mb-3 ${getThemeClass('text')} border-b pb-1`}>教育背景</h2>
               <div className="flex flex-col gap-4">
                  {data.education.map((edu, index) => (
                     <div key={index} className="relative group">
                        {isEditing && (
                           <div className="absolute -left-6 top-0">
                              <DeleteButton onClick={() => {
                                 const newEdu = [...data.education];
                                 newEdu.splice(index, 1);
                                 setData({...data, education: newEdu});
                              }} />
                           </div>
                        )}
                        <div className="flex justify-between font-bold text-slate-800">
                           <EditableField 
                              value={edu.institution}
                              onChange={(val: string) => {
                                 const newEdu = [...data.education];
                                 newEdu[index].institution = val;
                                 setData({...data, education: newEdu});
                              }}
                              isEditing={isEditing}
                              placeholder="学校"
                           />
                           <div className="text-sm font-normal text-slate-500 whitespace-nowrap">
                              <EditableField 
                                 value={edu.start}
                                 onChange={(val: string) => {
                                    const newEdu = [...data.education];
                                    newEdu[index].start = val;
                                    setData({...data, education: newEdu});
                                 }}
                                 isEditing={isEditing}
                                 placeholder="开始"
                              /> - <EditableField 
                                 value={edu.end}
                                 onChange={(val: string) => {
                                    const newEdu = [...data.education];
                                    newEdu[index].end = val;
                                    setData({...data, education: newEdu});
                                 }}
                                 isEditing={isEditing}
                                 placeholder="结束"
                              />
                           </div>
                        </div>
                        <div className="flex gap-2 text-sm text-slate-600">
                           <EditableField 
                              value={edu.major}
                              onChange={(val: string) => {
                                 const newEdu = [...data.education];
                                 newEdu[index].major = val;
                                 setData({...data, education: newEdu});
                              }}
                              isEditing={isEditing}
                              placeholder="主修专业"
                           />
                           <span>•</span>
                           <EditableField 
                              value={edu.degree}
                              onChange={(val: string) => {
                                 const newEdu = [...data.education];
                                 newEdu[index].degree = val;
                                 setData({...data, education: newEdu});
                              }}
                              isEditing={isEditing}
                              placeholder="学位"
                           />
                        </div>
                     </div>
                  ))}
                  <SectionControls 
                     isEditing={isEditing} 
                     onAdd={() => setData({...data, education: [...data.education, { institution: "学校名称", major: "主修专业", degree: "学位", start: "20xx", end: "20xx" }]})} 
                     label="教育经历"
                  />
               </div>
            </div>

            {/* Certificates */}
            <div className="col-span-1">
               <h2 className={`text-lg font-bold uppercase tracking-wider mb-3 ${getThemeClass('text')} border-b pb-1`}>证书</h2>
               <ul className="text-sm text-slate-600 flex flex-col gap-1.5">
                  {data.certificates.map((cert, index) => (
                     <li key={index} className="relative group pl-3">
                        <span className={`absolute left-0 top-2 w-1 h-1 rounded-full ${getThemeClass('bg').replace('bg-', 'bg-') === 'bg-slate-50' ? 'bg-slate-400' : 'bg-slate-400'}`}></span>
                        <div className="flex justify-between items-start">
                           <EditableField 
                              value={cert}
                              onChange={(val: string) => {
                                 const newCerts = [...data.certificates];
                                 newCerts[index] = val;
                                 setData({...data, certificates: newCerts});
                              }}
                              isEditing={isEditing}
                              multiline={true}
                              placeholder="证书名称"
                           />
                           {isEditing && <DeleteButton onClick={() => {
                              const newCerts = [...data.certificates];
                              newCerts.splice(index, 1);
                              setData({...data, certificates: newCerts});
                           }} />}
                        </div>
                     </li>
                  ))}
                  <SectionControls 
                     isEditing={isEditing} 
                     onAdd={() => setData({...data, certificates: [...data.certificates, "新证书"]})} 
                     label="证书"
                  />
               </ul>
            </div>
         </div>

         {/* Skills Section - Full Width Tag Cloud */}
         <div>
            <h2 className={`text-lg font-bold uppercase tracking-wider mb-3 ${getThemeClass('text')} border-b pb-1`}>专业技能</h2>
            <div className="flex flex-wrap gap-4">
                {Object.entries(data.skills).map(([category, items]) => (
                   <div key={category} className="flex-1 min-w-[200px]">
                      <h4 className="text-xs font-bold text-slate-400 uppercase mb-2">{
                         category === 'languages' ? '语言' : 
                         category === 'frontend' ? '前端' : 
                         category === 'backend' ? '后端' :
                         category === 'hardware' ? '硬件' : '其他'
                      }</h4>
                      <div className="flex flex-col gap-1">
                         {items.map((skill, idx) => (
                            <div key={idx} className="relative group text-sm text-slate-700">
                               <EditableField 
                                 value={skill}
                                 onChange={(val: string) => {
                                    const newSkills = {...data.skills};
                                    newSkills[category][idx] = val;
                                    setData({...data, skills: newSkills});
                                 }}
                                 isEditing={isEditing}
                                 className="border-b border-transparent hover:border-slate-300"
                               />
                               {isEditing && (
                                 <button 
                                    onClick={() => {
                                       const newSkills = {...data.skills};
                                       newSkills[category] = items.filter((_, i) => i !== idx);
                                       setData({...data, skills: newSkills});
                                    }}
                                    className="absolute -right-4 top-0.5 text-red-400 opacity-0 group-hover:opacity-100"
                                 >
                                    <X size={12} />
                                 </button>
                               )}
                            </div>
                         ))}
                         {isEditing && (
                            <button 
                              onClick={() => {
                                 const newSkills = {...data.skills};
                                 newSkills[category] = [...items, "新技能"];
                                 setData({...data, skills: newSkills});
                              }}
                              className="text-xs text-slate-400 flex items-center gap-1 mt-1"
                            >
                               <Plus size={10} /> 添加
                            </button>
                         )}
                      </div>
                   </div>
                ))}
            </div>
         </div>

         {/* Summary */}
         <div>
            <h2 className={`text-lg font-bold uppercase tracking-wider mb-3 ${getThemeClass('text')} border-b pb-1`}>个人简介</h2>
            <div className="text-sm leading-relaxed text-slate-600">
               <EditableField 
                  value={data.basics.summary} 
                  onChange={(val: string) => setData({...data, basics: {...data.basics, summary: val}})}
                  isEditing={isEditing}
                  multiline={true}
                  className="w-full block"
               />
            </div>
         </div>

         {/* Projects */}
         <div className="flex-1">
            <h2 className={`text-lg font-bold uppercase tracking-wider mb-4 ${getThemeClass('text')} border-b pb-1`}>项目经历</h2>
            <div className="flex flex-col space-y-5">
               {data.projects.map((project, index) => (
                  <div key={index} className="relative group">
                      {isEditing && (
                        <div className="absolute right-0 top-0 flex gap-1 bg-white p-1 shadow-sm opacity-0 group-hover:opacity-100 transition-opacity z-10 border border-slate-200 rounded">
                           <MoveButton 
                              onClick={() => moveProject(index, 'up')} 
                              direction="up" 
                              disabled={index === 0} 
                           />
                           <MoveButton 
                              onClick={() => moveProject(index, 'down')} 
                              direction="down" 
                              disabled={index === data.projects.length - 1} 
                           />
                           <DeleteButton onClick={() => {
                              const newProjects = [...data.projects];
                              newProjects.splice(index, 1);
                              setData({...data, projects: newProjects});
                           }} />
                        </div>
                      )}
                      <div className="flex justify-between items-baseline mb-1">
                        <div className="flex items-center gap-2">
                           <h3 className="font-bold text-slate-800 text-base">
                              <EditableField 
                                 value={project.name}
                                 onChange={(val: string) => {
                                    const newProjects = [...data.projects];
                                    newProjects[index].name = val;
                                    setData({...data, projects: newProjects});
                                 }}
                                 isEditing={isEditing}
                                 placeholder="项目名称"
                              />
                           </h3>
                           <span className={`text-xs px-2 py-0.5 rounded ${getThemeClass('bg')} ${getThemeClass('text')}`}>
                              <EditableField 
                                 value={project.role}
                                 onChange={(val: string) => {
                                    const newProjects = [...data.projects];
                                    newProjects[index].role = val;
                                    setData({...data, projects: newProjects});
                                 }}
                                 isEditing={isEditing}
                                 placeholder="角色"
                              />
                           </span>
                        </div>
                        <div className="text-sm font-medium text-slate-500">
                           <EditableField 
                              value={project.date}
                              onChange={(val: string) => {
                                 const newProjects = [...data.projects];
                                 newProjects[index].date = val;
                                 setData({...data, projects: newProjects});
                              }}
                              isEditing={isEditing}
                              placeholder="时间"
                           />
                        </div>
                      </div>
                      
                      <div className="text-sm text-slate-600 mb-2 italic">
                         <EditableField 
                              value={project.desc}
                              onChange={(val: string) => {
                                 const newProjects = [...data.projects];
                                 newProjects[index].desc = val;
                                 setData({...data, projects: newProjects});
                              }}
                              isEditing={isEditing}
                              multiline={true}
                              placeholder="项目简述"
                           />
                      </div>

                      <ul className="text-sm text-slate-600 list-disc list-inside space-y-1 ml-1">
                        {(project.details || []).map((detail, dIdx) => (
                           <li key={dIdx} className="relative group pl-2 -indent-2">
                              <EditableField 
                                 value={detail}
                                 onChange={(val: string) => {
                                    const newProjects = [...data.projects];
                                    newProjects[index].details[dIdx] = val;
                                    setData({...data, projects: newProjects});
                                 }}
                                 isEditing={isEditing}
                                 multiline={true}
                                 placeholder="详情"
                              />
                              {isEditing && (
                                 <button 
                                    onClick={() => {
                                       const newProjects = [...data.projects];
                                       newProjects[index].details = project.details.filter((_, i) => i !== dIdx);
                                       setData({...data, projects: newProjects});
                                    }}
                                    className="absolute right-0 top-0 text-red-400 opacity-0 group-hover:opacity-100"
                                 >
                                    <X size={12} />
                                 </button>
                              )}
                           </li>
                        ))}
                         <li className="list-none">
                           {isEditing && (
                              <button 
                                 onClick={() => {
                                    const newProjects = [...data.projects];
                                    if(!newProjects[index].details) newProjects[index].details = [];
                                    newProjects[index].details.push("新项目详情");
                                    setData({...data, projects: newProjects});
                                 }}
                                 className="text-xs text-slate-400 flex items-center gap-1 mt-1 ml-4"
                              >
                                 <Plus size={10} /> 添加详情
                              </button>
                           )}
                         </li>
                      </ul>
                  </div>
               ))}
               <SectionControls 
                  isEditing={isEditing} 
                  onAdd={() => setData({...data, projects: [...data.projects, {
                     name: "新项目",
                     role: "角色",
                     date: "20xx.xx",
                     desc: "项目简述",
                     details: ["项目详情"]
                  }]})} 
                  label="项目经历"
               />
            </div>
         </div>
      </div>
   );
};

// --- App ---

const App = () => {
  const [isEditing, setIsEditing] = useState(false);
  const [resumeData, setResumeData] = useState<ResumeData>(initialResumeData);
  const [isPrinting, setIsPrinting] = useState(false);
  const [currentTemplate, setCurrentTemplate] = useState(TEMPLATES.modern);
  const [themeColor, setThemeColor] = useState(THEME_COLORS.blue);

  useEffect(() => {
    const saved = localStorage.getItem("resumeData");
    if (saved) {
      try {
        setResumeData(JSON.parse(saved));
      } catch (e) {
        console.error("Failed to load saved resume data", e);
      }
    }
  }, []);

  useEffect(() => {
    localStorage.setItem("resumeData", JSON.stringify(resumeData));
  }, [resumeData]);

  const handlePrint = async () => {
    if (isEditing) {
       // Auto-save and exit edit mode before printing
       setIsEditing(false);
    }
    
    setIsPrinting(true);
    // Slight delay to ensure React state updates and DOM renders without edit controls
    setTimeout(() => {
      window.print();
      setIsPrinting(false);
    }, 500);
  };

  const toggleEdit = () => {
    if (isEditing) {
      if (confirm("确定要退出编辑模式吗？")) {
        setIsEditing(false);
      }
    } else {
      setIsEditing(true);
    }
  };

  const exportData = () => {
     const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(resumeData));
     const downloadAnchorNode = document.createElement('a');
     downloadAnchorNode.setAttribute("href", dataStr);
     downloadAnchorNode.setAttribute("download", "resume_data.json");
     document.body.appendChild(downloadAnchorNode);
     downloadAnchorNode.click();
     downloadAnchorNode.remove();
  };

  const importData = (e: any) => {
     const file = e.target.files[0];
     if (!file) return;
     const reader = new FileReader();
     reader.onload = (event) => {
        try {
           if (event.target?.result) {
               const obj = JSON.parse(event.target.result as string);
               // Basic validation could go here
               setResumeData(obj);
               alert("数据导入成功！");
           }
        } catch(err) {
           alert("导入失败：无效的 JSON 文件");
        }
     };
     reader.readAsText(file);
  };

  // Reordering function
  const moveProject = (index: number, direction: 'up' | 'down') => {
    const newProjects = [...resumeData.projects];
    if (direction === 'up' && index > 0) {
      [newProjects[index], newProjects[index - 1]] = [newProjects[index - 1], newProjects[index]];
    } else if (direction === 'down' && index < newProjects.length - 1) {
      [newProjects[index], newProjects[index + 1]] = [newProjects[index + 1], newProjects[index]];
    }
    setResumeData({ ...resumeData, projects: newProjects });
  };

  return (
    <div className="min-h-screen pb-20 md:pb-0">
      {/* Floating Toolbar */}
      <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 flex items-center gap-2 bg-white/90 backdrop-blur shadow-2xl p-2 rounded-2xl border border-slate-200 no-print transition-all hover:scale-105">
        <button 
          onClick={toggleEdit}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl font-bold transition-all ${
            isEditing 
              ? "bg-red-500 text-white shadow-lg shadow-red-500/30" 
              : "bg-slate-800 text-white shadow-lg shadow-slate-800/30"
          }`}
        >
          {isEditing ? <><LogOut size={18} /> 退出编辑</> : <><Edit2 size={18} /> 编辑内容</>}
        </button>

        <div className="w-px h-8 bg-slate-200 mx-1"></div>

        <button 
          onClick={handlePrint}
          className="p-2.5 text-slate-600 hover:bg-slate-100 rounded-xl transition-colors"
          title="导出 PDF"
        >
          <Download size={20} />
        </button>

        <div className="relative group">
           <button className="p-2.5 text-slate-600 hover:bg-slate-100 rounded-xl transition-colors" title="备份/恢复">
              <Save size={20} />
           </button>
           <div className="absolute bottom-full mb-2 left-1/2 -translate-x-1/2 bg-white rounded-lg shadow-xl border border-slate-100 overflow-hidden hidden group-hover:flex flex-col w-32">
              <button onClick={exportData} className="px-4 py-2 text-left text-sm hover:bg-slate-50 flex items-center gap-2">
                 <Download size={14} /> 导出备份
              </button>
              <label className="px-4 py-2 text-left text-sm hover:bg-slate-50 flex items-center gap-2 cursor-pointer">
                 <UploadCloud size={14} /> 恢复备份
                 <input type="file" accept=".json" onChange={importData} className="hidden" />
              </label>
           </div>
        </div>

        <div className="w-px h-8 bg-slate-200 mx-1"></div>

        {/* Template Switcher */}
        <div className="flex bg-slate-100 rounded-xl p-1">
           <button 
             onClick={() => setCurrentTemplate(TEMPLATES.modern)}
             className={`p-1.5 rounded-lg transition-all ${currentTemplate === TEMPLATES.modern ? 'bg-white shadow text-slate-800' : 'text-slate-400 hover:text-slate-600'}`}
             title="现代侧边栏"
           >
              <Layout size={18} />
           </button>
           <button 
             onClick={() => setCurrentTemplate(TEMPLATES.classic)}
             className={`p-1.5 rounded-lg transition-all ${currentTemplate === TEMPLATES.classic ? 'bg-white shadow text-slate-800' : 'text-slate-400 hover:text-slate-600'}`}
             title="经典简约"
           >
              <FileJson size={18} />
           </button>
        </div>

        {/* Color Picker */}
        <div className="relative group mx-1">
           <button className={`w-8 h-8 rounded-full border-2 border-white shadow flex items-center justify-center ${
              themeColor === THEME_COLORS.blue ? 'bg-blue-500' : 
              themeColor === THEME_COLORS.emerald ? 'bg-emerald-500' : 
              themeColor === THEME_COLORS.violet ? 'bg-violet-500' : 
              themeColor === THEME_COLORS.rose ? 'bg-rose-500' : 'bg-slate-500'
           }`}>
              <Palette size={14} className="text-white" />
           </button>
           <div className="absolute bottom-full mb-4 left-1/2 -translate-x-1/2 flex gap-2 bg-white p-2 rounded-xl shadow-xl border border-slate-100 hidden group-hover:flex">
              {Object.entries(THEME_COLORS).map(([name, value]) => (
                 <button 
                   key={name}
                   onClick={() => setThemeColor(value)}
                   className={`w-6 h-6 rounded-full border border-slate-200 transition-transform hover:scale-110 ${
                      value === 'blue' ? 'bg-blue-500' : 
                      value === 'emerald' ? 'bg-emerald-500' : 
                      value === 'violet' ? 'bg-violet-500' : 
                      value === 'rose' ? 'bg-rose-500' : 'bg-slate-500'
                   }`}
                 />
              ))}
           </div>
        </div>

      </div>

      <div className="max-w-[210mm] mx-auto my-8 print:my-0 print:w-full print:max-w-none">
         {currentTemplate === TEMPLATES.modern ? (
            <ModernTemplate 
               data={resumeData} 
               setData={setResumeData} 
               isEditing={isEditing} 
               themeColor={themeColor} 
               moveProject={moveProject}
            />
         ) : (
            <ClassicTemplate 
               data={resumeData} 
               setData={setResumeData} 
               isEditing={isEditing} 
               themeColor={themeColor} 
               moveProject={moveProject}
            />
         )}
      </div>
    </div>
  );
};

const container = document.getElementById("root");
const root = createRoot(container!);
root.render(<App />);