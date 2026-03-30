import DOMPurify from 'dompurify'
import { marked } from 'marked'

marked.setOptions({
  gfm: true,
  breaks: true,
})

export const renderMarkdown = (content: string) => {
  const html = marked.parse(content || '') as string
  return DOMPurify.sanitize(html)
}
