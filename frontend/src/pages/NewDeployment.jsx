import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Navbar } from '../components/Navbar';
import { FormField } from '../components/FormField';
import { Button } from '../components/Button';
import { createDeployment } from '../api/deployments';
import './NewDeployment.css';

export const NewDeployment = () => {
  const navigate = useNavigate();
  const [repoUrl, setRepoUrl] = useState('');
  const [branch, setBranch] = useState('main');
  const [framework, setFramework] = useState('');
  const [buildCommand, setBuildCommand] = useState('');
  const [outputDir, setOutputDir] = useState('');
  
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleFrameworkChange = (e) => {
    const val = e.target.value;
    setFramework(val);
    switch (val) {
      case 'React':
        setBuildCommand('npm run build');
        setOutputDir('dist');
        break;
      case 'Vue':
        setBuildCommand('npm run build');
        setOutputDir('dist');
        break;
      case 'Angular':
        setBuildCommand('npm run build');
        setOutputDir('dist');
        break;
      case 'SvelteKit':
        setBuildCommand('npm run build');
        setOutputDir('build');
        break;
      case 'Next.js':
        setBuildCommand('npm run build');
        setOutputDir('out');
        break;
      case 'Other':
      default:
        setBuildCommand('');
        setOutputDir('');
        break;
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const deployData = { repoUrl, branch, buildCommand, outputDir };
      const res = await createDeployment(deployData);
      navigate(`/deployments/${res.deploymentId}`);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create deployment');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="new-deploy page-enter">
      <Navbar />
      <main className="new-deploy__content">
        <form className="new-deploy__form" onSubmit={handleSubmit}>
          
          <div className="new-deploy__section">
            <div className="new-deploy__step-counter">01</div>
            <h2 className="new-deploy__section-title">Repository</h2>
            <FormField 
              label="Git Repository URL" 
              id="repoUrl" 
              value={repoUrl}
              onChange={e => setRepoUrl(e.target.value)}
              placeholder="https://github.com/you/your-project"
              hint="> must be a public repository"
              required
              disabled={loading}
            />
            <FormField 
              label="Branch" 
              id="branch" 
              value={branch}
              onChange={e => setBranch(e.target.value)}
              required
              disabled={loading}
            />
          </div>

          <div className="new-deploy__section">
            <div className="new-deploy__step-counter">02</div>
            <h2 className="new-deploy__section-title">Build Config</h2>
            
            <div className="form-field">
              <label htmlFor="framework" className="form-field__label">Framework</label>
              <div className="custom-select-wrapper">
                <select 
                  id="framework" 
                  className="custom-select" 
                  value={framework} 
                  onChange={handleFrameworkChange}
                  required
                  disabled={loading}
                >
                  <option value="" disabled>Select framework</option>
                  <option value="React">React (dist)</option>
                  <option value="Vue">Vue (dist)</option>
                  <option value="Angular">Angular (dist)</option>
                  <option value="SvelteKit">SvelteKit (build)</option>
                  <option value="Next.js">Next.js static (out)</option>
                  <option value="Other">Other (custom)</option>
                </select>
              </div>
            </div>

            <FormField 
              label="Build Command" 
              id="buildCommand" 
              value={buildCommand}
              onChange={e => setBuildCommand(e.target.value)}
              required
              disabled={loading}
            />
            <FormField 
              label="Output Directory" 
              id="outputDir" 
              value={outputDir}
              onChange={e => setOutputDir(e.target.value)}
              required
              disabled={loading}
            />
          </div>

          <div className="new-deploy__section">
            <div className="new-deploy__step-counter">03</div>
            <h2 className="new-deploy__section-title">Review</h2>
            
            <div className="new-deploy__review-block">
              <div className="review-row"><span className="review-label">repo</span>{repoUrl || '...'}</div>
              <div className="review-row"><span className="review-label">branch</span>{branch || '...'}</div>
              <div className="review-row"><span className="review-label">command</span>{buildCommand || '...'}</div>
              <div className="review-row"><span className="review-label">output</span>{outputDir || '...'}</div>
            </div>
          </div>

          {error && <div className="new-deploy__error">{error}</div>}

          <Button type="submit" loading={loading} style={{ width: '100%', marginTop: '3rem' }}>
            → Deploy Now
          </Button>

        </form>
      </main>
    </div>
  );
};
