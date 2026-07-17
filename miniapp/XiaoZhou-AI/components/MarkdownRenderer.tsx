import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface MarkdownRendererProps {
  content: string;
}

const MarkdownRenderer: React.FC<MarkdownRendererProps> = ({ content }) => {
  return (
    <div className="prose prose-sm md:prose-base dark:prose-invert max-w-none break-words leading-relaxed">
      <ReactMarkdown 
        remarkPlugins={[remarkGfm]}
        components={{
          code({node, className, children, ...props}) {
            const match = /language-(\w+)/.exec(className || '')
            const isInline = !match && !String(children).includes('\n');
            
            return !isInline ? (
              <div className="rounded-lg overflow-hidden my-2 bg-gray-800 border border-gray-700">
                <div className="flex items-center justify-between px-3 py-1 bg-gray-900 text-xs text-gray-400 border-b border-gray-700">
                  <span>{match?.[1] || 'code'}</span>
                </div>
                <div className="p-3 overflow-x-auto">
                  <code className={`${className} text-sm text-gray-200 font-mono`} {...props}>
                    {children}
                  </code>
                </div>
              </div>
            ) : (
              <code className="bg-gray-200 dark:bg-gray-700 rounded px-1 py-0.5 text-sm font-mono text-pink-600 dark:text-pink-400" {...props}>
                {children}
              </code>
            )
          },
          a: ({node, ...props}) => <a className="text-blue-500 hover:underline" target="_blank" rel="noopener noreferrer" {...props} />,
          p: ({node, ...props}) => <p className="mb-2 last:mb-0" {...props} />,
          ul: ({node, ...props}) => <ul className="list-disc pl-4 mb-2" {...props} />,
          ol: ({node, ...props}) => <ol className="list-decimal pl-4 mb-2" {...props} />,
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
};

export default MarkdownRenderer;