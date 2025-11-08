/**
 * Lethality Assessment Program (LAP)
 *
 * Maryland Model - Quick screening tool for law enforcement and hotline use
 * 11 critical questions for immediate risk assessment
 */

import React, { useState, useEffect } from 'react';
import type { LethalityScreeningResponses, RiskLevel } from '../../utils/types';

interface LAPProps {
  responses: LethalityScreeningResponses;
  onChange: (responses: LethalityScreeningResponses) => void;
  onScoreCalculated: (score: number, maxScore: number, riskLevel: RiskLevel) => void;
}

const LAP_QUESTIONS = [
  {
    id: 'LAP1',
    question: 'Has he/she ever used a weapon against you or threatened you with a weapon?',
    criticalQuestion: true,
  },
  {
    id: 'LAP2',
    question: 'Is he/she threatening to kill you or your children?',
    criticalQuestion: true,
  },
  {
    id: 'LAP3',
    question: 'Do you think he/she is capable of killing you?',
    criticalQuestion: true,
  },
  {
    id: 'LAP4',
    question: 'Has he/she ever tried to choke (strangle) you?',
    criticalQuestion: true,
  },
  {
    id: 'LAP5',
    question: 'Is he/she violently and constantly jealous of you?',
    criticalQuestion: false,
  },
  {
    id: 'LAP6',
    question: 'Have you left him/her or separated after living together or being married?',
    criticalQuestion: false,
  },
  {
    id: 'LAP7',
    question: 'Is he/she unemployed?',
    criticalQuestion: false,
  },
  {
    id: 'LAP8',
    question: 'Has he/she ever tried to kill himself/herself?',
    criticalQuestion: false,
  },
  {
    id: 'LAP9',
    question: 'Do you have a child that he/she knows is not his/hers?',
    criticalQuestion: false,
  },
  {
    id: 'LAP10',
    question: 'Does he/she force you to have sex when you don\'t want to?',
    criticalQuestion: false,
  },
  {
    id: 'LAP11',
    question: 'Does he/she own a gun?',
    criticalQuestion: false,
  },
];

export const LAP: React.FC<LAPProps> = ({ responses, onChange, onScoreCalculated }) => {
  const [localResponses, setLocalResponses] = useState<LethalityScreeningResponses>(responses || {});

  useEffect(() => {
    setLocalResponses(responses || {});
  }, [responses]);

  const handleResponseChange = (questionId: string, answer: boolean) => {
    const question = LAP_QUESTIONS.find(q => q.id === questionId);
    if (!question) return;

    const updatedResponses = {
      ...localResponses,
      [questionId]: {
        question: question.question,
        answer,
        weight: 1,
      },
    };

    setLocalResponses(updatedResponses);
    onChange(updatedResponses);

    // Calculate score
    calculateScore(updatedResponses);
  };

  const calculateScore = (currentResponses: LethalityScreeningResponses) => {
    let yesCount = 0;
    let criticalYesCount = 0;

    LAP_QUESTIONS.forEach(question => {
      const response = currentResponses[question.id];
      if (response && response.answer === true) {
        yesCount++;
        if (question.criticalQuestion) {
          criticalYesCount++;
        }
      }
    });

    // Determine risk level
    let riskLevel: RiskLevel;

    // If ANY critical question (1-4) is YES → Immediate HIGH risk
    if (criticalYesCount > 0) {
      riskLevel = 'HIGH';
    }
    // 4+ YES responses → HIGH risk
    else if (yesCount >= 4) {
      riskLevel = 'HIGH';
    }
    // 1-3 YES responses → MODERATE risk
    else if (yesCount >= 1) {
      riskLevel = 'MODERATE';
    }
    // 0 YES responses → LOW risk (but still provide resources)
    else {
      riskLevel = 'LOW';
    }

    onScoreCalculated(yesCount, 11, riskLevel);
  };

  const answeredCount = Object.keys(localResponses).length;
  const totalQuestions = LAP_QUESTIONS.length;
  const progressPercent = (answeredCount / totalQuestions) * 100;

  const yesCount = LAP_QUESTIONS.reduce((total, q) => {
    const response = localResponses[q.id];
    return total + (response?.answer === true ? 1 : 0);
  }, 0);

  const criticalYesCount = LAP_QUESTIONS.filter(
    q => q.criticalQuestion && localResponses[q.id]?.answer === true
  ).length;

  return (
    <div className="lethality-tool lap">
      <div className="tool-header">
        <h4>Lethality Assessment Program (LAP)</h4>
        <p className="tool-description">
          Quick screening tool for immediate risk assessment. The first 4 questions are{' '}
          <strong>critical indicators</strong> - any "Yes" answer requires immediate intervention.
        </p>
      </div>

      <div className="progress-indicator">
        <div className="progress-bar">
          <div className="progress-fill" style={{ width: `${progressPercent}%` }} />
        </div>
        <span className="progress-text">
          {answeredCount} of {totalQuestions} questions answered
        </span>
      </div>

      {criticalYesCount > 0 && (
        <div className="alert alert-danger" role="alert">
          <span className="alert-icon">⚠️</span>
          <span>
            <strong>CRITICAL RISK DETECTED:</strong> {criticalYesCount} critical indicator(s)
            identified. Immediate high-risk intervention recommended.
          </span>
        </div>
      )}

      <div className="questions-list">
        {LAP_QUESTIONS.map((question, index) => (
          <div
            key={question.id}
            className={`question-item ${question.criticalQuestion ? 'critical-question' : ''}`}
          >
            <div className="question-header">
              <span className="question-number">{index + 1}</span>
              <span className="question-text">
                {question.question}
                {question.criticalQuestion && (
                  <span className="critical-badge">Critical Question</span>
                )}
              </span>
            </div>

            <div className="question-answers">
              <label className="radio-label">
                <input
                  type="radio"
                  name={question.id}
                  checked={localResponses[question.id]?.answer === true}
                  onChange={() => handleResponseChange(question.id, true)}
                />
                <span>Yes</span>
              </label>

              <label className="radio-label">
                <input
                  type="radio"
                  name={question.id}
                  checked={localResponses[question.id]?.answer === false}
                  onChange={() => handleResponseChange(question.id, false)}
                />
                <span>No</span>
              </label>
            </div>

            {question.criticalQuestion && localResponses[question.id]?.answer === true && (
              <div className="critical-alert">
                <span className="alert-icon">🚨</span>
                <span>Critical risk indicator - immediate action required</span>
              </div>
            )}
          </div>
        ))}
      </div>

      {answeredCount === totalQuestions && (
        <div className="assessment-summary">
          <div className="score-display">
            <span className="score-label">YES Responses:</span>
            <span className="score-value">
              {yesCount} / {totalQuestions}
            </span>
          </div>

          {criticalYesCount > 0 && (
            <div className="critical-summary">
              <h5>Critical Indicators:</h5>
              <p>
                {criticalYesCount} critical indicator(s) identified. This client requires immediate
                high-risk intervention and safety planning.
              </p>
            </div>
          )}

          <div className="score-interpretation">
            <h5>Risk Level Interpretation:</h5>
            <ul>
              <li>
                <strong>ANY critical question (1-4) = YES:</strong> HIGH risk (immediate
                intervention)
              </li>
              <li>
                <strong>4+ YES responses:</strong> HIGH risk (immediate intervention)
              </li>
              <li>
                <strong>1-3 YES responses:</strong> MODERATE risk (resources and support needed)
              </li>
              <li>
                <strong>0 YES responses:</strong> LOW risk (still provide resources)
              </li>
            </ul>
          </div>

          <div className="next-steps">
            <h5>Recommended Next Steps:</h5>
            {yesCount >= 4 || criticalYesCount > 0 ? (
              <ul>
                <li>Immediate safety planning session</li>
                <li>Emergency shelter placement assessment</li>
                <li>Referral to domestic violence hotline</li>
                <li>Law enforcement notification (if client consents)</li>
                <li>Coordinate with crisis intervention team</li>
              </ul>
            ) : yesCount >= 1 ? (
              <ul>
                <li>Safety planning discussion</li>
                <li>Provide domestic violence resources</li>
                <li>Schedule follow-up within 7 days</li>
                <li>Coordinate with case manager</li>
              </ul>
            ) : (
              <ul>
                <li>Provide domestic violence resources</li>
                <li>Establish regular check-ins</li>
                <li>Monitor for escalation</li>
              </ul>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
