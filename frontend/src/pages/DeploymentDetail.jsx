import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Navbar } from '../components/Navbar';
import { StatusDot } from '../components/StatusDot';
import { TerminalBlock } from '../components/TerminalBlock';
import { BuildStepLog } from '../components/BuildStepLog';
import { getDeployment, getDeploymentJobs } from '../api/deployments';
import './DeploymentDetail.css';

export const DeploymentDetail = () => {
  const { id } = useParams();
  const [deployment, setDeployment] = useState(null);
  const [jobs, setJobs] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    let mounted = true;
    let timer = null;

    const fetchDetail = async () => {
      try {
        const deployData = await getDeployment(id);
        if (!mounted) return;
        setDeployment(deployData);
        
        try {
          const jobsData = await getDeploymentJobs(id);
          if (!mounted) return;
          setJobs(jobsData.steps || []);
        } catch (jobErr) {
          if (jobErr.response?.status === 404) {
            setJobs([]); // No logs yet, wait for worker
          } else {
            throw jobErr;
          }
        }

        const isLiveOrFailed = ['LIVE', 'FAILED'].includes(deployData.status);
        if (!isLiveOrFailed) {
          timer = setTimeout(fetchDetail, 3000);
        }
      } catch (err) {
        if (!mounted) return;
        if (err.response?.status !== 401) {
          setError(err.response?.data?.message || 'Failed to fetch deployment details');
        }
      }
    };

    fetchDetail();

    return () => {
      mounted = false;
      if (timer) clearTimeout(timer);
    };
  }, [id]);

  if (error) {
    return (
      <div className="detail-page page-enter">
        <Navbar />
        <main className="detail-page__content">
          <div className="detail-page__error">{error}</div>
        </main>
      </div>
    );
  }

  if (!deployment) {
    return (
      <div className="detail-page page-enter">
        <Navbar />
        <main className="detail-page__content">
          <div className="detail-page__loading">
            {'>'} Loading deployment...<span className="terminal-cursor">▋</span>
          </div>
        </main>
      </div>
    );
  }

  const isBuilding = !['LIVE', 'FAILED'].includes(deployment.status);

  return (
    <div className="detail-page page-enter">
       <Navbar />
       <main className="detail-page__content">
         <header className="detail-page__header">
            <Link to="/dashboard" className="detail-page__back">← Back</Link>
            <h1 className="detail-page__title">Deployment {deployment.deploymentId.substring(0, 8)}</h1>
         </header>

         <section className="detail-page__metadata">
            <div className="metadata-item">
              <span className="metadata-label">STATUS</span>
              <span className="metadata-value detail-page__status">
                <StatusDot status={deployment.status} />
                {deployment.status}
              </span>
            </div>
            <div className="metadata-item">
              <span className="metadata-label">REPO</span>
              <span className="metadata-value">{deployment.repoUrl}</span>
            </div>
            <div className="metadata-item">
              <span className="metadata-label">COMMAND</span>
              <span className="metadata-value">{deployment.buildCommand}</span>
            </div>
            <div className="metadata-item">
              <span className="metadata-label">OUTPUT DIR</span>
              <span className="metadata-value">{deployment.outputDir}</span>
            </div>
            {deployment.status === 'LIVE' && deployment.siteUrl && (
              <div className="metadata-item">
                <span className="metadata-label">URL</span>
                <a href={deployment.siteUrl} target="_blank" rel="noopener noreferrer" className="metadata-link">
                  → Visit Site
                </a>
              </div>
            )}
         </section>

         <section className="detail-page__terminal-section">
            <div className="muted-label">// build_logs</div>
            <TerminalBlock 
              error={deployment.status === 'FAILED' ? deployment.errorMessage : null}
              isBuilding={isBuilding}
            >
              {jobs.map((job) => (
                <BuildStepLog 
                  key={job.id}
                  step={job.step}
                  logOutput={job.logOutput}
                  startedAt={job.startedAt}
                  finishedAt={job.finishedAt}
                />
              ))}
              {!isBuilding && deployment.status === 'LIVE' && (
                <div className="detail-page__sys-msg">
                  <br/>
                  ── SYSTEM ──────────────────────────────────────<br/>
                  Deployment successful. Site is live.
                </div>
              )}
            </TerminalBlock>
         </section>
       </main>
    </div>
  );
};
