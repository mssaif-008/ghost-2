import React, { useEffect, useState, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Navbar } from '../components/Navbar';
import { DeploymentRow } from '../components/DeploymentRow';
import { getDeployments, deleteDeployment } from '../api/deployments';
import './Dashboard.css';

export const Dashboard = () => {
  const [deployments, setDeployments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchDeployments = async () => {
    try {
      const data = await getDeployments();
      setDeployments(data);
      if (loading) setLoading(false);
    } catch (err) {
      if (err.response?.status !== 401) {
        setError(err.response?.data?.message || 'Failed to fetch deployments');
      }
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDeployments();
    const interval = setInterval(fetchDeployments, 5000);
    return () => clearInterval(interval);
  }, []);

  const handleDelete = async (id) => {
    try {
      await deleteDeployment(id);
      setDeployments(prev => prev.filter(d => d.deploymentId !== id));
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete deployment');
    }
  };

  const liveCount = deployments.filter(d => d.status === 'LIVE').length;
  const failedCount = deployments.filter(d => d.status === 'FAILED').length;

  return (
    <div className="dashboard page-enter">
      <Navbar />
      <main className="dashboard__content">
        <header className="dashboard__header">
          <div className="dashboard__header-text">
            <h1>Your Deployments</h1>
            <div className="dashboard__stats">
              {deployments.length} deployments · {liveCount} live · {failedCount} failed
            </div>
          </div>
          <Link to="/deploy/new" className="btn btn--primary dashboard__new-btn">→ New Deployment</Link>
        </header>

        {error && <div className="dashboard__error">{error}</div>}

        {loading ? (
          <div className="dashboard__loading">
            {'>'} Fetching deployments...<span className="terminal-cursor">▋</span>
          </div>
        ) : deployments.length === 0 ? (
          <div className="dashboard__empty">
            <h2 className="dashboard__empty-text">"Nothing deployed yet."</h2>
            <Link to="/deploy/new" className="dashboard__empty-link">→ Create your first deployment</Link>
          </div>
        ) : (
          <div className="dashboard__list">
            <div className="dashboard__list-header">
              <div className="deployment-row__col deployment-row__status">STATUS</div>
              <div className="deployment-row__col deployment-row__id">ID</div>
              <div className="deployment-row__col deployment-row__repo">REPOSITORY</div>
              <div className="deployment-row__col deployment-row__date">CREATED</div>
              <div className="deployment-row__col deployment-row__actions"></div>
            </div>
            {deployments.map((dep, idx) => (
              <DeploymentRow 
                key={dep.deploymentId} 
                deployment={dep} 
                index={idx} 
                onDelete={handleDelete} 
              />
            ))}
          </div>
        )}
      </main>
    </div>
  );
};
