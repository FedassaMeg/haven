import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Alert, AlertDescription } from '@/components/ui/alert';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
  FormDescription
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Shield,
  AlertTriangle,
  Eye,
  EyeOff,
  Info,
  Clock,
  DollarSign,
  Heart,
  Users,
  Home,
  Save,
  RotateCcw
} from 'lucide-react';

// Form validation schema with conditional logic
const intakePsdeSchema = z.object({
  informationDate: z.string().min(1, 'Information date is required'),
  collectionStage: z.string().min(1, 'Collection stage is required'),

  // Income fields
  totalMonthlyIncome: z.number().min(0).max(999999).optional(),
  incomeFromAnySource: z.string().optional(),
  isEarnedIncomeImputed: z.boolean().optional(),
  isOtherIncomeImputed: z.boolean().optional(),

  // Health insurance
  coveredByHealthInsurance: z.string().optional(),
  noInsuranceReason: z.string().optional(),
  hasVawaProtectedHealthInfo: z.boolean().optional(),

  // Disabilities
  physicalDisability: z.string().optional(),
  developmentalDisability: z.string().optional(),
  chronicHealthCondition: z.string().optional(),
  hivAids: z.string().optional(),
  mentalHealthDisorder: z.string().optional(),
  substanceUseDisorder: z.string().optional(),
  hasDisabilityRelatedVawaInfo: z.boolean().optional(),

  // Domestic violence
  domesticViolence: z.string().optional(),
  domesticViolenceRecency: z.string().optional(),
  currentlyFleeingDomesticViolence: z.string().optional(),
  dvRedactionLevel: z.string().optional(),
  vawaConfidentialityRequested: z.boolean().optional(),

  // RRH move-in
  residentialMoveInDate: z.string().optional(),
  moveInType: z.string().optional(),
  isSubsidizedByRrh: z.boolean().optional(),
}).refine((data) => {
  // Conditional validation: DV logic
  if (data.domesticViolence === 'NO') {
    return !data.domesticViolenceRecency || data.domesticViolenceRecency === 'DATA_NOT_COLLECTED';
  }
  if (data.domesticViolence === 'YES') {
    return data.domesticViolenceRecency && data.domesticViolenceRecency !== 'DATA_NOT_COLLECTED';
  }
  return true;
}, {
  message: "Domestic violence recency is required when DV history is 'Yes', and should not be provided when 'No'",
  path: ['domesticViolenceRecency']
});

type IntakePsdeFormData = z.infer<typeof intakePsdeSchema>;

interface IntakePsdeFormProps {
  enrollmentId: string;
  clientId: string;
  onSubmit: (data: IntakePsdeFormData) => Promise<void>;
  onCancel: () => void;
  initialData?: Partial<IntakePsdeFormData>;
  userRoles: string[];
}

export function IntakePsdeForm({
  enrollmentId,
  clientId,
  onSubmit,
  onCancel,
  initialData,
  userRoles
}: IntakePsdeFormProps) {
  const [isDvSectionVisible, setIsDvSectionVisible] = useState(false);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const [showVawaWarning, setShowVawaWarning] = useState(false);

  // Check user permissions
  const canAccessDvData = userRoles.some(role =>
    ['DV_SPECIALIST', 'ADMIN', 'CASE_MANAGER'].includes(role));
  const canAccessSensitiveData = userRoles.some(role =>
    ['DV_SPECIALIST', 'ADMIN'].includes(role));

  const form = useForm<IntakePsdeFormData>({
    resolver: zodResolver(intakePsdeSchema),
    defaultValues: {
      informationDate: new Date().toISOString().split('T')[0],
      collectionStage: 'COMPREHENSIVE_ASSESSMENT',
      ...initialData
    }
  });

  const { watch, setValue, getValues } = form;
  const watchedValues = watch();

  // Auto-save functionality
  useEffect(() => {
    const subscription = watch(() => setHasUnsavedChanges(true));
    return () => subscription.unsubscribe();
  }, [watch]);

  // Conditional logic effects
  useEffect(() => {
    const dvValue = watchedValues.domesticViolence;

    // Clear recency if DV is NO
    if (dvValue === 'NO') {
      setValue('domesticViolenceRecency', 'DATA_NOT_COLLECTED');
      setValue('currentlyFleeingDomesticViolence', 'DATA_NOT_COLLECTED');
    }

    // Show VAWA warning for high-risk cases
    const isHighRisk = dvValue === 'YES' &&
      (watchedValues.currentlyFleeingDomesticViolence === 'YES' ||
       watchedValues.domesticViolenceRecency === 'WITHIN_3_MONTHS');
    setShowVawaWarning(isHighRisk);
  }, [watchedValues.domesticViolence, watchedValues.currentlyFleeingDomesticViolence,
      watchedValues.domesticViolenceRecency, setValue]);

  // Income validation effect
  useEffect(() => {
    const incomeFromAny = watchedValues.incomeFromAnySource;
    const totalIncome = watchedValues.totalMonthlyIncome;

    if (incomeFromAny === 'NO' && totalIncome && totalIncome > 0) {
      setValue('totalMonthlyIncome', 0);
    }
  }, [watchedValues.incomeFromAnySource, watchedValues.totalMonthlyIncome, setValue]);

  const handleSubmit = async (data: IntakePsdeFormData) => {
    try {
      await onSubmit(data);
      setHasUnsavedChanges(false);
    } catch (error) {
      console.error('Error submitting PSDE data:', error);
    }
  };

  const handleReset = () => {
    form.reset();
    setHasUnsavedChanges(false);
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      {/* Header with compliance info */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center space-x-2">
              <Users className="h-5 w-5" />
              <span>Program-Specific Data Elements (PSDE)</span>
              <Badge variant="outline">HUD/VAWA Compliant</Badge>
            </CardTitle>
            <div className="flex items-center space-x-2">
              {hasUnsavedChanges && (
                <Badge variant="secondary" className="flex items-center space-x-1">
                  <Clock className="h-3 w-3" />
                  <span>Unsaved changes</span>
                </Badge>
              )}
              {showVawaWarning && (
                <Badge variant="destructive" className="flex items-center space-x-1">
                  <Shield className="h-3 w-3" />
                  <span>High-risk DV case</span>
                </Badge>
              )}
            </div>
          </div>

          {/* Compliance notice */}
          <Alert>
            <Info className="h-4 w-4" />
            <AlertDescription>
              This form collects Program-Specific Data Elements required by HUD.
              Information marked as sensitive is protected under VAWA confidentiality requirements.
            </AlertDescription>
          </Alert>
        </CardHeader>
      </Card>

      <Form {...form}>
        <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-6">

          {/* Assessment Metadata */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Info className="h-4 w-4" />
                <span>Assessment Information</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="informationDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Information Date *</FormLabel>
                    <FormControl>
                      <Input
                        type="date"
                        {...field}
                        aria-describedby="informationDate-help"
                      />
                    </FormControl>
                    <FormDescription id="informationDate-help">
                      Date this information was collected
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="collectionStage"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Collection Stage *</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger aria-describedby="collectionStage-help">
                          <SelectValue placeholder="Select collection stage" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="INITIAL_INTAKE">Initial Intake</SelectItem>
                        <SelectItem value="COMPREHENSIVE_ASSESSMENT">Comprehensive Assessment</SelectItem>
                        <SelectItem value="PSDE_COLLECTION">PSDE Collection</SelectItem>
                        <SelectItem value="ANNUAL_ASSESSMENT">Annual Assessment</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormDescription id="collectionStage-help">
                      Stage of data collection process
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          {/* Income Section */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <DollarSign className="h-4 w-4" />
                <span>Income & Benefits (HMIS 4.02-4.03)</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="incomeFromAnySource"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Income from Any Source *</FormLabel>
                      <Select onValueChange={field.onChange} defaultValue={field.value}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="Select..." />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="YES">Yes</SelectItem>
                          <SelectItem value="NO">No</SelectItem>
                          <SelectItem value="CLIENT_DOESNT_KNOW">Client doesn't know</SelectItem>
                          <SelectItem value="CLIENT_REFUSED">Client refused</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="totalMonthlyIncome"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Total Monthly Income</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          min="0"
                          max="999999"
                          {...field}
                          onChange={(e) => field.onChange(e.target.value ? Number(e.target.value) : undefined)}
                          disabled={watchedValues.incomeFromAnySource === 'NO'}
                        />
                      </FormControl>
                      <FormDescription>
                        {watchedValues.incomeFromAnySource === 'NO' &&
                          "Automatically set to 0 when no income from any source"}
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            </CardContent>
          </Card>

          {/* Health Insurance Section */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Heart className="h-4 w-4" />
                <span>Health Insurance (HMIS 4.04)</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <FormField
                control={form.control}
                name="coveredByHealthInsurance"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Covered by Health Insurance *</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select..." />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="YES">Yes</SelectItem>
                        <SelectItem value="NO">No</SelectItem>
                        <SelectItem value="CLIENT_DOESNT_KNOW">Client doesn't know</SelectItem>
                        <SelectItem value="CLIENT_REFUSED">Client refused</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {canAccessSensitiveData && (
                <FormField
                  control={form.control}
                  name="hasVawaProtectedHealthInfo"
                  render={({ field }) => (
                    <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                      <FormControl>
                        <Checkbox
                          checked={field.value}
                          onCheckedChange={field.onChange}
                        />
                      </FormControl>
                      <div className="space-y-1 leading-none">
                        <FormLabel className="flex items-center space-x-2">
                          <Shield className="h-3 w-3" />
                          <span>Contains VAWA-protected health information</span>
                        </FormLabel>
                        <FormDescription>
                          Check if health information is related to domestic violence
                        </FormDescription>
                      </div>
                    </FormItem>
                  )}
                />
              )}
            </CardContent>
          </Card>

          {/* Disability Section */}
          <Card>
            <CardHeader>
              <CardTitle>Disability Information (HMIS 4.05-4.10)</CardTitle>
              <FormDescription>
                All disability fields are required for HMIS data quality compliance
              </FormDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {[
                  { name: 'physicalDisability', label: 'Physical Disability' },
                  { name: 'developmentalDisability', label: 'Developmental Disability' },
                  { name: 'chronicHealthCondition', label: 'Chronic Health Condition' },
                  { name: 'hivAids', label: 'HIV/AIDS' },
                  { name: 'mentalHealthDisorder', label: 'Mental Health Disorder' },
                  { name: 'substanceUseDisorder', label: 'Substance Use Disorder' }
                ].map((disability) => (
                  <FormField
                    key={disability.name}
                    control={form.control}
                    name={disability.name as keyof IntakePsdeFormData}
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>{disability.label} *</FormLabel>
                        <Select onValueChange={field.onChange} defaultValue={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Select..." />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="YES">Yes</SelectItem>
                            <SelectItem value="NO">No</SelectItem>
                            <SelectItem value="CLIENT_DOESNT_KNOW">Client doesn't know</SelectItem>
                            <SelectItem value="CLIENT_REFUSED">Client refused</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                ))}
              </div>
            </CardContent>
          </Card>

          {/* Domestic Violence Section - Conditional Access */}
          {canAccessDvData && (
            <Card className="border-orange-200">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle className="flex items-center space-x-2">
                    <Shield className="h-4 w-4 text-orange-600" />
                    <span>Domestic Violence (HMIS 4.11)</span>
                    <Badge variant="outline" className="bg-orange-50">VAWA Protected</Badge>
                  </CardTitle>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => setIsDvSectionVisible(!isDvSectionVisible)}
                  >
                    {isDvSectionVisible ? (
                      <>
                        <EyeOff className="h-4 w-4 mr-2" />
                        Hide DV Section
                      </>
                    ) : (
                      <>
                        <Eye className="h-4 w-4 mr-2" />
                        Show DV Section
                      </>
                    )}
                  </Button>
                </div>

                {showVawaWarning && (
                  <Alert className="border-red-200 bg-red-50">
                    <AlertTriangle className="h-4 w-4 text-red-600" />
                    <AlertDescription className="text-red-800">
                      <strong>High-Risk DV Case Detected:</strong> This client may be currently fleeing
                      domestic violence or experienced recent violence. Enhanced confidentiality measures
                      are recommended. Consider consulting with DV specialist.
                    </AlertDescription>
                  </Alert>
                )}
              </CardHeader>

              {isDvSectionVisible && (
                <CardContent className="space-y-4">
                  <FormField
                    control={form.control}
                    name="domesticViolence"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Domestic Violence History *</FormLabel>
                        <Select onValueChange={field.onChange} defaultValue={field.value}>
                          <FormControl>
                            <SelectTrigger>
                              <SelectValue placeholder="Select..." />
                            </SelectTrigger>
                          </FormControl>
                          <SelectContent>
                            <SelectItem value="YES">Yes</SelectItem>
                            <SelectItem value="NO">No</SelectItem>
                            <SelectItem value="CLIENT_DOESNT_KNOW">Client doesn't know</SelectItem>
                            <SelectItem value="CLIENT_REFUSED">Client refused</SelectItem>
                          </SelectContent>
                        </Select>
                        <FormMessage />
                      </FormItem>
                    )}
                  />

                  {watchedValues.domesticViolence === 'YES' && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 p-4 bg-orange-50 rounded-lg">
                      <FormField
                        control={form.control}
                        name="domesticViolenceRecency"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>When was DV experienced? *</FormLabel>
                            <Select onValueChange={field.onChange} defaultValue={field.value}>
                              <FormControl>
                                <SelectTrigger>
                                  <SelectValue placeholder="Select..." />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                <SelectItem value="WITHIN_3_MONTHS">Within past 3 months</SelectItem>
                                <SelectItem value="THREE_TO_SIX_MONTHS">3-6 months ago</SelectItem>
                                <SelectItem value="SIX_TO_12_MONTHS">6-12 months ago</SelectItem>
                                <SelectItem value="MORE_THAN_12_MONTHS">More than 12 months ago</SelectItem>
                                <SelectItem value="CLIENT_DOESNT_KNOW">Client doesn't know</SelectItem>
                                <SelectItem value="CLIENT_REFUSED">Client refused</SelectItem>
                              </SelectContent>
                            </Select>
                            <FormMessage />
                          </FormItem>
                        )}
                      />

                      <FormField
                        control={form.control}
                        name="currentlyFleeingDomesticViolence"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Currently fleeing DV?</FormLabel>
                            <Select onValueChange={field.onChange} defaultValue={field.value}>
                              <FormControl>
                                <SelectTrigger>
                                  <SelectValue placeholder="Select..." />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                <SelectItem value="YES">Yes</SelectItem>
                                <SelectItem value="NO">No</SelectItem>
                                <SelectItem value="CLIENT_DOESNT_KNOW">Client doesn't know</SelectItem>
                                <SelectItem value="CLIENT_REFUSED">Client refused</SelectItem>
                              </SelectContent>
                            </Select>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                  )}

                  {canAccessSensitiveData && (
                    <div className="space-y-4 p-4 bg-gray-50 rounded-lg">
                      <FormField
                        control={form.control}
                        name="vawaConfidentialityRequested"
                        render={({ field }) => (
                          <FormItem className="flex flex-row items-start space-x-3 space-y-0">
                            <FormControl>
                              <Checkbox
                                checked={field.value}
                                onCheckedChange={field.onChange}
                              />
                            </FormControl>
                            <div className="space-y-1 leading-none">
                              <FormLabel>VAWA confidentiality requested by victim</FormLabel>
                              <FormDescription>
                                Client has specifically requested enhanced confidentiality protections
                              </FormDescription>
                            </div>
                          </FormItem>
                        )}
                      />

                      <FormField
                        control={form.control}
                        name="dvRedactionLevel"
                        render={({ field }) => (
                          <FormItem>
                            <FormLabel>Redaction Level</FormLabel>
                            <Select onValueChange={field.onChange} defaultValue={field.value}>
                              <FormControl>
                                <SelectTrigger>
                                  <SelectValue placeholder="Select redaction level..." />
                                </SelectTrigger>
                              </FormControl>
                              <SelectContent>
                                <SelectItem value="NO_REDACTION">No redaction required</SelectItem>
                                <SelectItem value="REDACT_FOR_GENERAL_STAFF">Redact for general staff</SelectItem>
                                <SelectItem value="REDACT_FOR_NON_DV_SPECIALISTS">Redact for non-DV specialists</SelectItem>
                                <SelectItem value="VICTIM_REQUESTED_CONFIDENTIALITY">Victim requested confidentiality</SelectItem>
                              </SelectContent>
                            </Select>
                            <FormDescription>
                              Controls who can access this DV information
                            </FormDescription>
                            <FormMessage />
                          </FormItem>
                        )}
                      />
                    </div>
                  )}
                </CardContent>
              )}
            </Card>
          )}

          {/* RRH Move-in Section */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center space-x-2">
                <Home className="h-4 w-4" />
                <span>RRH Move-in Information</span>
              </CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="residentialMoveInDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Residential Move-in Date</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} />
                    </FormControl>
                    <FormDescription>
                      Date client moved into permanent housing
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="moveInType"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Move-in Type</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Select move-in type..." />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="INITIAL_MOVE_IN">Initial move-in</SelectItem>
                        <SelectItem value="SUBSEQUENT_MOVE_IN">Subsequent move</SelectItem>
                        <SelectItem value="MOVE_IN_AFTER_TEMPORARY_ABSENCE">After temporary absence</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </CardContent>
          </Card>

          {/* Form Actions */}
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <Button type="button" variant="outline" onClick={handleReset}>
                    <RotateCcw className="h-4 w-4 mr-2" />
                    Reset Form
                  </Button>
                  {hasUnsavedChanges && (
                    <Badge variant="secondary">Unsaved changes</Badge>
                  )}
                </div>

                <div className="flex items-center space-x-2">
                  <Button type="button" variant="outline" onClick={onCancel}>
                    Cancel
                  </Button>
                  <Button type="submit" className="flex items-center space-x-2">
                    <Save className="h-4 w-4" />
                    <span>Save PSDE Data</span>
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </form>
      </Form>
    </div>
  );
}