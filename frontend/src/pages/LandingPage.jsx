import React from 'react';
import { Link } from 'react-router-dom';
import './LandingPage.css';

export const LandingPage = () => {
  return (
    <div className="landing page-enter">
      <section className="landing__hero">
        <div className="landing__hero-text">
          <h1>Deploy without ceremony.</h1>
          <p>Ghost Host builds your frontend, hosts it on a subdomain, and gets out of the way.</p>
          <div className="landing__cta-group">
            <Link to="/signup" className="btn btn--primary">→ Start Deploying</Link>
            <Link to="/login" className="btn btn--ghost">Sign In</Link>
          </div>
        </div>
        <div className="landing__hero-ascii">
          <pre>{`┌──────────────┐
│   Browser    │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  Nginx Edge  │
└──────┬───────┘
   ┌───┴───┐
   ▼       ▼
┌───────┐ ┌─────────┐
│  API  │ │ Supabase│
└───────┘ └─────────┘`}</pre>
        </div>
      </section>
      
      <section className="landing__features">
        <h2 className="section-heading">The infrastructure. All of it.</h2>
        <div className="landing__features-grid">
          <div className="feature-card">
            <h3>⬡ Isolated builds</h3>
            <p>Docker per deploy, CPU/memory capped</p>
          </div>
          <div className="feature-card">
            <h3>◈ Instant subdomains</h3>
            <p>UUID becomes your subdomain, zero config</p>
          </div>
          <div className="feature-card">
            <h3>⟐ Build logs</h3>
            <p>Step-by-step terminal output per deploy</p>
          </div>
        </div>
      </section>

      <section className="landing__arch">
        <div className="landing__arch-flow">
          Browser → Nginx Edge → API / Supabase Storage
        </div>
      </section>

      <section className="landing__banner">
        <h2>Your code. Your server. Your rules.</h2>
        <Link to="/signup" className="btn btn--primary">→ Get Started</Link>
      </section>
    </div>
  );
};
