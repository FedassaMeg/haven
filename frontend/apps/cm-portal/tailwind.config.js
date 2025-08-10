/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,ts,jsx,tsx,mdx}",
    "../../packages/ui/src/**/*.{js,ts,jsx,tsx}",
    "../../packages/auth/src/**/*.{js,ts,jsx,tsx}",
  ],
  presets: [require("../../packages/ui/tailwind.config.js")],
};