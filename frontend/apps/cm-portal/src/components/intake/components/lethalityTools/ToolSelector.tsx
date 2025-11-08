/**
 * Lethality Tool Selector
 *
 * Dynamically renders the selected lethality screening tool
 */

import React from 'react';
import type { LethalityScreeningTool, LethalityScreeningResponses, RiskLevel } from '../../utils/types';
import { DangerAssessment } from './DangerAssessment';
import { LAP } from './LAP';

interface LethalityToolRendererProps {
  tool: LethalityScreeningTool;
  responses: LethalityScreeningResponses;
  onChange: (responses: LethalityScreeningResponses) => void;
  onScoreCalculated: (score: number, maxScore: number, riskLevel: RiskLevel) => void;
}

export const LethalityToolRenderer: React.FC<LethalityToolRendererProps> = ({
  tool,
  responses,
  onChange,
  onScoreCalculated,
}) => {
  switch (tool) {
    case 'DANGER_ASSESSMENT':
      return (
        <DangerAssessment
          responses={responses}
          onChange={onChange}
          onScoreCalculated={onScoreCalculated}
        />
      );

    case 'LAP':
      return (
        <LAP
          responses={responses}
          onChange={onChange}
          onScoreCalculated={onScoreCalculated}
        />
      );

    case 'ODARA':
      return (
        <div className="tool-placeholder">
          <div className="alert alert-info">
            <h4>Ontario Domestic Assault Risk Assessment (ODARA)</h4>
            <p>
              The ODARA is a 13-item actuarial tool designed for law enforcement. This tool
              requires specific training and access to criminal history databases.
            </p>
            <p>
              <strong>For implementation:</strong> Contact your supervisor or training coordinator
              for ODARA certification.
            </p>
          </div>
        </div>
      );

    case 'DVSI':
      return (
        <div className="tool-placeholder">
          <div className="alert alert-info">
            <h4>Domestic Violence Screening Instrument (DVSI)</h4>
            <p>
              The DVSI is a 12-item weighted assessment tool used primarily in pretrial and
              probation settings.
            </p>
            <p>
              <strong>For implementation:</strong> This tool requires specialized training and is
              typically administered by criminal justice professionals.
            </p>
          </div>
        </div>
      );

    case 'MOSAIC':
      return (
        <div className="tool-placeholder">
          <div className="alert alert-warning">
            <h4>MOSAIC Threat Assessment</h4>
            <p>
              MOSAIC is a comprehensive computerized threat assessment system that requires
              professional training and specialized software.
            </p>
            <p>
              <strong>Access Required:</strong> Contact your organization's MOSAIC administrator or
              supervisor for access and training.
            </p>
            <p className="mt-2">
              <a
                href="https://www.mosaicmethod.com"
                target="_blank"
                rel="noopener noreferrer"
                className="external-link"
              >
                Learn more about MOSAIC Method →
              </a>
            </p>
          </div>
        </div>
      );

    case 'OTHER':
      return (
        <div className="tool-placeholder">
          <div className="alert alert-info">
            <h4>Other Validated Tool</h4>
            <p>
              If you are using a different validated lethality assessment tool, please document the
              tool name and results in the assessor notes section.
            </p>
            <div className="form-field">
              <label>Tool Name</label>
              <input
                type="text"
                placeholder="Enter the name of the assessment tool used"
                className="form-control"
              />
            </div>
            <div className="form-field">
              <label>Assessment Results Summary</label>
              <textarea
                placeholder="Summarize the assessment results, score, and risk level"
                rows={4}
                className="form-control"
              />
            </div>
          </div>
        </div>
      );

    case 'NONE':
      return (
        <div className="tool-placeholder">
          <div className="alert alert-warning">
            <h4>Manual Risk Assessment</h4>
            <p>
              No standardized tool selected. Please conduct a thorough clinical assessment and
              document your findings in the assessor notes section.
            </p>
            <p className="mt-2">
              <strong>Note:</strong> Using a validated lethality assessment tool is strongly
              recommended for consistency and accuracy in risk assessment.
            </p>
            <div className="form-field">
              <label>Manual Assessment Notes</label>
              <textarea
                placeholder="Document your clinical assessment, risk factors identified, and rationale for risk level determination"
                rows={6}
                className="form-control"
              />
            </div>
          </div>
        </div>
      );

    default:
      return null;
  }
};
