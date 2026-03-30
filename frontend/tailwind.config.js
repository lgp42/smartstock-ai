/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        darkBg: '#07101E',
        darkCard: '#0C1828',
        darkCardHover: '#142038',
        darkBorder: '#182840',
        primary: '#22D3EE',     // cyan-400 — electric, technical
        secondary: '#10B981',
        upPrice: '#EF4444',     // Red for up (A-Share style)
        downPrice: '#10B981',   // Green for down
        accent: '#F59E0B',      // amber — for highlights
      },
      fontFamily: {
        sans: ['DM Sans', 'system-ui', 'sans-serif'],
        display: ['Syne', 'sans-serif'],
        mono: ['JetBrains Mono', 'Menlo', 'monospace'],
      },
      backgroundImage: {
        'grid-pattern': "url(\"data:image/svg+xml,%3Csvg width='40' height='40' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M40 0H0v40' fill='none' stroke='rgba(34,211,238,0.03)' stroke-width='1'/%3E%3C/svg%3E\")",
      },
      keyframes: {
        'price-pulse': {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0.6' },
        },
        'slide-in': {
          '0%': { opacity: '0', transform: 'translateY(6px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        'glow-pulse': {
          '0%, 100%': { boxShadow: '0 0 8px rgba(34,211,238,0.3)' },
          '50%': { boxShadow: '0 0 20px rgba(34,211,238,0.6)' },
        },
      },
      animation: {
        'price-pulse': 'price-pulse 1.5s ease-in-out infinite',
        'slide-in': 'slide-in 0.3s ease-out both',
        'glow-pulse': 'glow-pulse 2s ease-in-out infinite',
      },
    },
  },
  plugins: [],
}
