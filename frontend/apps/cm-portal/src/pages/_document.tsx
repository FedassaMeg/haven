import { Html, Head, Main, NextScript } from 'next/document';
import { Toaster } from '@haven/ui';

export default function Document() {
  return (
    <Html lang="en">
      <Head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap"
          rel="stylesheet"
        />
        <meta name="description" content="Haven Case Management Portal" />
        <link rel="icon" href="/favicon.ico" />
      </Head>
      <body>
        <Main />
        <Toaster />
        <NextScript />
      </body>
    </Html>
  );
}