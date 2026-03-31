import React from 'react';
import { Link } from 'react-router-dom';
import { StatusDot } from './StatusDot';
import './DeploymentRow.css';

export const DeploymentRow = ({ deployment, onDelete, index }) => {
  const { deploymentId: id, repoUrl, status, createdAt } = deployment;
  const shortId = id.substring(0, 8);
  const shortRepo = repoUrl.replace(/^https?:\/\//, '').replace(/^github\.com\//, '').substring(0, 35);
  const dateFormatted = new Date(createdAt).toLocaleString();

  return (
    <div className="deployment-row page-enter" style={{ animationDelay: `${index * 50}ms` }}>
      <div className="deployment-row__col deployment-row__status">
        <StatusDot status={status} />
        {status}
      </div>
      <div className="deployment-row__col deployment-row__id">{shortId}</div>
      <div className="deployment-row__col deployment-row__repo">{shortRepo}</div>
      <div className="deployment-row__col deployment-row__date">{dateFormatted}</div>
      <div className="deployment-row__col deployment-row__actions">
        <Link to={`/deployments/${id}`} className="deployment-row__action">→ View</Link>
        <button 
          className="deployment-row__action deployment-row__action--danger" 
          onClick={() => {
            if (window.confirm('Delete this deployment?')) {
              onDelete(id);
            }
          }}>
          ⊗ Delete
        </button>
      </div>
    </div>
  );
};
