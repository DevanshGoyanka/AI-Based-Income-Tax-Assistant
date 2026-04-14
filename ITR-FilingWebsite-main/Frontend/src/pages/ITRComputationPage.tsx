import { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAY } from '../contexts/AYContext';
import { itrApi } from '../lib/api/itr';
import { clientsApi } from '../lib/api/clients';
import { Spinner } from '../components/ui/Spinner';
import toast from 'react-hot-toast';

import { 
  BusinessTab, 
  OtherSourcesTab, 
  VDATab, 
  DeductionsTab, 
  LossesTab, 
  TDSTab, 
  TaxComputationTab,
  computeTax 
} from './ITRComputationTabs';

export default function ITRComputationPage() {
  const { clientId, year } = useParams();
  const navigate = useNavigate();
  const { ayParam } = useAY();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState(0);
  const [regime, setRegime] = useState<'old' | 'new'>('new');
  const [itrForm, setItrForm] = useState('ITR-1');
  const [showImportMenu, setShowImportMenu] = useState(false);
  const [clientData, setClientData] = useState<any>(null);
  const [formData, setFormData] = useState<any>({
    basic: 0, da: 0, hra: 0, bonus: 0, allowances: 0, perquisites: 0,
    hraRent: 0, hraMetro: false, profTax: 0,
    hpType: 'self', grossRent: 0, munTax: 0, homeLoanInt: 0, sopLoanInt: 0,
    stcgPre: 0, stcgPost: 0, stcgOther: 0, ltcgPre: 0, ltcgPost: 0, ltcgOther: 0,
    bizPresumptive: '44AD', bizTurnover: 0, bizDeclared: 0, bpNetProfit: 0,
    interestSB: 0, interestFD: 0, dividends: 0, familyPension: 0, otherMisc: 0,
    vdaGains: 0,
    s80C_epf: 0, s80C_ppf: 0, s80C_elss: 0, s80C_lic: 0, s80C_home: 0,
    s80CCD1B: 0, s80CCD2: 0, s80D_self: 0, s80D_parent: 0, s80E: 0, s80TTA: 0, s80G: 0,
    bfLoss: 0,
    tdsS192: 0, tds194A: 0, tdsOther: 0,
    adv15Jun: 0, adv15Sep: 0, adv15Dec: 0, adv15Mar: 0, selfTax: 0,
    age: 30
  });

  useEffect(() => {
    if (!clientId) return;
    setLoading(true);
    Promise.all([
      clientsApi.get(Number(clientId)),
      itrApi.getFormData(Number(clientId), ayParam || '2025-26')
    ])
      .then(([client, itrData]) => {
        setClientData(client);
        setFormData((prev: any) => ({ 
          ...prev, 
          ...itrData,
          name: client.name,
          pan: client.pan,
          email: client.email,
          mobile: client.mobile,
          aadhaar: client.aadhaar,
          dob: client.dob
        }));
      })
      .catch(err => toast.error(err.message))
      .finally(() => setLoading(false));
  }, [clientId, ayParam]);

  const taxResult = useMemo(() => computeTax(formData, year || '2025-26', regime), [formData, year, regime]);

  useEffect(() => {
    autoDetectITRForm();
  }, [formData.basic, formData.bizTurnover, formData.bpNetProfit, formData.stcgPre, formData.stcgPost, formData.ltcgPre, formData.ltcgPost, formData.grossRent, formData.interestFD, formData.dividends]);

  const handleSave = async () => {
    setSaving(true);
    try {
      await itrApi.saveFormData(Number(clientId), year!, formData);
      toast.success('Saved ✓');
    } catch (err: any) {
      toast.error(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleDownloadJson = () => {
    itrApi.downloadJson(Number(clientId), year!).catch(err => toast.error(err.message));
  };

  const handleDownloadPdf = async () => {
    try {
      await itrApi.downloadPdf(Number(clientId), year!);
      toast.success('PDF downloaded successfully');
    } catch (err: any) {
      toast.error(err.message || 'PDF download failed');
    }
  };

  const handleFileImport = async (type: string, file: File) => {
    try {
      toast.loading(`Importing ${type}...`);
      
      if (type === 'form16-pdf' || type === 'form16-json') {
        const data = await import('../lib/api/integration').then(m => m.integrationApi.extractForm16(file));
        const populated = await import('../lib/api/integration').then(m => m.integrationApi.autoPopulateFromForm16(formData, data));
        setFormData((prev: any) => ({ ...prev, ...populated }));
        toast.dismiss();
        toast.success('Form 16 imported and auto-populated');
      } else if (type === 'ais-pdf' || type === 'tis-pdf' || type === '26as' || type === 'prefill') {
        let data;
        
        // Format DOB for PDF decryption (DDMMYYYY)
        const formatDobForDecryption = (dob: string) => {
          // Assuming dob is in YYYY-MM-DD format from backend
          const [year, month, day] = dob.split('-');
          return `${day}${month}${year}`;
        };
        
        const pan = clientData?.pan;
        const dob = clientData?.dob ? formatDobForDecryption(clientData.dob) : undefined;
        
        if (type === 'prefill') {
          // For prefill, parse JSON directly on frontend to avoid backend DTO mismatch
          const text = await file.text();
          data = JSON.parse(text);
        } else if (type === 'ais-pdf') {
          data = await import('../lib/api/integration').then(m => m.integrationApi.importAIS(file, pan, dob));
        } else if (type === 'tis-pdf') {
          data = await import('../lib/api/integration').then(m => m.integrationApi.importTIS(file, pan, dob));
        } else if (type === '26as') {
          data = await import('../lib/api/integration').then(m => m.integrationApi.import26AS(file, pan, dob));
        }
        
        // Validate PAN matches
        const docPan = data.personalInfo?.pan || data.personalInfo?.assesseVerPan || data.pan;
        if (docPan && docPan !== clientData?.pan) {
          toast.dismiss();
          toast.error(`PAN mismatch: Document PAN (${docPan}) does not match client PAN (${clientData?.pan})`);
          setShowImportMenu(false);
          return;
        }
        
        // Auto-populate personal info and income data
        if (type === 'ais-pdf' || type === 'tis-pdf') {
          const populated = await import('../lib/api/integration').then(m => m.integrationApi.autoPopulateFromAIS(formData, data));
          setFormData((prev: any) => ({ ...prev, ...populated }));
        } else if (type === 'prefill') {
          // ITD Prefill - map actual JSON structure
          console.log('Prefill data received:', data);
          
          const mappedData: any = {};
          
          // Personal Info
          if (data.personalInfo) {
            const pi = data.personalInfo;
            mappedData.pan = pi.pan || pi.assesseVerPan || formData.pan;
            mappedData.name = pi.assesseeVerName || pi.assesseeName?.firstName + ' ' + pi.assesseeName?.surNameOrOrgName || formData.name;
            mappedData.dob = pi.dob || formData.dob;
            mappedData.email = pi.address?.emailAddress || formData.email;
            mappedData.mobile = pi.address?.mobileNo?.toString() || formData.mobile;
            
            // Decode base64 aadhaar
            if (pi.aadhaarCardNo) {
              try {
                const decoded = atob(pi.aadhaarCardNo);
                mappedData.aadhaar = decoded.replace(/\D/g, '').slice(0, 12);
              } catch {
                mappedData.aadhaar = pi.aadhaarCardNo;
              }
            }
            
            // Address fields
            if (pi.address) {
              mappedData.flatNo = pi.address.residenceNo || '';
              mappedData.premises = pi.address.residenceName || '';
              mappedData.road = pi.address.roadOrStreet || '';
              mappedData.area = pi.address.localityOrArea || '';
              mappedData.city = pi.address.cityOrTownOrDistrict || '';
              mappedData.pincode = pi.address.pinCode?.toString() || '';
            }
          }
          
          // Income from insights section
          if (data.insights) {
            mappedData.interestSB = data.insights.intrstFrmSavingBank || 0;
            mappedData.interestFD = data.insights.intrstFrmTermDeposit || 0;
            
            if (data.insights.scheduleOS?.incOthThanOwnRaceHorse) {
              mappedData.dividends = data.insights.scheduleOS.incOthThanOwnRaceHorse.dividendGross || 0;
            }
            
            // Deductions
            if (data.insights.UsrDeductUndChapVIAType) {
              mappedData.s80TTA = data.insights.UsrDeductUndChapVIAType.Section80TTB || 0;
            }
          }
          
          // TDS from form26as
          if (data.form26as) {
            if (data.form26as.tdsOnOthThanSals?.tdSonOthThanSal) {
              const tds = data.form26as.tdsOnOthThanSals.tdSonOthThanSal[0];
              if (tds) {
                mappedData.tds194A = tds.taxDeductCreditDtls?.taxClaimedOwnHands || 0;
              }
            }
            
            // Tax payments
            if (data.form26as.taxPayments?.taxPayment) {
              const payment = data.form26as.taxPayments.taxPayment[0];
              if (payment) {
                mappedData.selfTax = payment.amt || 0;
              }
            }
          }
          
          console.log('Mapped data:', mappedData);
          
          setFormData((prev: any) => ({ 
            ...prev, 
            ...mappedData
          }));
        } else {
          setFormData((prev: any) => ({ ...prev, ...data }));
        }
        
        toast.dismiss();
        toast.success(`${type.toUpperCase()} imported and validated`);
      }
      setShowImportMenu(false);
    } catch (err: any) {
      toast.dismiss();
      toast.error(err.message || 'Import failed');
    }
  };

  const autoDetectITRForm = () => {
    // Auto-detect based on income sources
    const hasBusinessIncome = formData.bizTurnover > 0 || formData.bpNetProfit > 0;
    const hasCapitalGains = formData.stcgPre > 0 || formData.stcgPost > 0 || formData.ltcgPre > 0 || formData.ltcgPost > 0;
    const hasHouseProperty = formData.hpType === 'letout' && formData.grossRent > 0;
    const totalIncome = formData.basic + formData.interestFD + formData.dividends;

    let detectedForm = 'ITR-1';
    if (hasBusinessIncome && formData.bizPresumptive !== 'Regular') {
      detectedForm = 'ITR-4';
    } else if (hasBusinessIncome) {
      detectedForm = 'ITR-3';
    } else if (hasCapitalGains || hasHouseProperty || totalIncome > 5000000) {
      detectedForm = 'ITR-2';
    }

    // Only show toast if form actually changed
    if (detectedForm !== itrForm) {
      setItrForm(detectedForm);
      toast(`Auto-detected: ${detectedForm}`, { icon: '🔍' });
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
        <Spinner size={32} />
      </div>
    );
  }

  const tabs = [
    '📋 Personal Info',
    '💼 Salary Income',
    '🏠 House Property',
    '📈 Capital Gains',
    '🏪 Business',
    '💰 Other Sources',
    '₿ VDA / Crypto',
    '➖ Deductions',
    '📉 Losses B/F',
    '🧾 TDS & Advance Tax',
    '🧮 Tax Computation'
  ];

  return (
    <div>
      <div style={{
        background: 'white',
        padding: '16px 24px',
        marginBottom: 16,
        borderRadius: 'var(--radius)',
        border: '1px solid var(--border)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <button
              onClick={() => navigate('/filing')}
              style={{
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                fontSize: 18,
                color: 'var(--text-secondary)'
              }}
            >
              ←
            </button>
            <div>
              <div style={{ fontSize: 18, fontWeight: 600, color: 'var(--text-primary)' }}>
                {clientData?.name || 'Loading...'}
              </div>
              <div style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 2 }}>
                <span className="mono">{clientData?.pan || ''}</span>
                <span style={{ margin: '0 8px' }}>•</span>
                <span>AY {ayParam || '2025-26'}</span>
              </div>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <select
              value={itrForm}
              onChange={(e) => setItrForm(e.target.value)}
              style={{
                padding: '6px 12px',
                border: '1px solid var(--border)',
                borderRadius: 6,
                fontSize: 13,
                fontWeight: 500,
                background: 'white'
              }}
            >
              <option value="ITR-1">ITR-1</option>
              <option value="ITR-2">ITR-2</option>
              <option value="ITR-3">ITR-3</option>
              <option value="ITR-4">ITR-4</option>
            </select>
            <select
              value={regime}
              onChange={(e) => setRegime(e.target.value as any)}
              style={{
                padding: '6px 12px',
                border: '1px solid var(--border)',
                borderRadius: 6,
                fontSize: 13,
                fontWeight: 500,
                background: 'white'
              }}
            >
              <option value="old">Old Regime</option>
              <option value="new">New Regime</option>
            </select>
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, paddingLeft: 34 }}>

          <div style={{ position: 'relative' }}>
            <button
              onClick={() => setShowImportMenu(!showImportMenu)}
              style={{
                padding: '6px 12px',
                background: 'var(--info)',
                color: 'white',
                border: 'none',
                borderRadius: 6,
                fontSize: 12,
                cursor: 'pointer'
              }}
            >
              Import
            </button>
            {showImportMenu && (
              <div style={{
                position: 'absolute',
                top: '100%',
                left: 0,
                marginTop: 4,
                background: 'white',
                border: '1px solid var(--border)',
                borderRadius: 6,
                boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                zIndex: 1000,
                minWidth: 200
              }}>
                <label style={{
                  display: 'block',
                  padding: '8px 12px',
                  fontSize: 12,
                  cursor: 'pointer',
                  borderBottom: '1px solid var(--border)'
                }}>
                  <input
                    type="file"
                    accept=".pdf"
                    onChange={(e) => e.target.files?.[0] && handleFileImport('form16-pdf', e.target.files[0])}
                    style={{ display: 'none' }}
                  />
                  Form 16 PDF
                </label>
                <label style={{
                  display: 'block',
                  padding: '8px 12px',
                  fontSize: 12,
                  cursor: 'pointer',
                  borderBottom: '1px solid var(--border)'
                }}>
                  <input
                    type="file"
                    accept=".pdf"
                    onChange={(e) => e.target.files?.[0] && handleFileImport('ais-pdf', e.target.files[0])}
                    style={{ display: 'none' }}
                  />
                  AIS PDF
                </label>
                <label style={{
                  display: 'block',
                  padding: '8px 12px',
                  fontSize: 12,
                  cursor: 'pointer',
                  borderBottom: '1px solid var(--border)'
                }}>
                  <input
                    type="file"
                    accept=".pdf"
                    onChange={(e) => e.target.files?.[0] && handleFileImport('tis-pdf', e.target.files[0])}
                    style={{ display: 'none' }}
                  />
                  TIS PDF
                </label>
                <label style={{
                  display: 'block',
                  padding: '8px 12px',
                  fontSize: 12,
                  cursor: 'pointer',
                  borderBottom: '1px solid var(--border)'
                }}>
                  <input
                    type="file"
                    accept=".pdf,.json"
                    onChange={(e) => e.target.files?.[0] && handleFileImport('26as', e.target.files[0])}
                    style={{ display: 'none' }}
                  />
                  Form 26AS (PDF/JSON)
                </label>
                <label style={{
                  display: 'block',
                  padding: '8px 12px',
                  fontSize: 12,
                  cursor: 'pointer'
                }}>
                  <input
                    type="file"
                    accept=".json"
                    onChange={(e) => e.target.files?.[0] && handleFileImport('prefill', e.target.files[0])}
                    style={{ display: 'none' }}
                  />
                  ITD Prefill JSON
                </label>
              </div>
            )}
          </div>

          <button
            onClick={handleSave}
            disabled={saving}
            style={{
              padding: '6px 12px',
              background: saving ? 'var(--border)' : 'var(--gold)',
              color: 'white',
              border: 'none',
              borderRadius: 6,
              fontSize: 12,
              fontWeight: 500,
              cursor: saving ? 'not-allowed' : 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: 6
            }}
          >
            {saving && <Spinner size={12} />}
            Save
          </button>

          <button
            onClick={handleDownloadJson}
            style={{
              padding: '6px 12px',
              background: 'var(--accent-blue)',
              color: 'white',
              border: 'none',
              borderRadius: 6,
              fontSize: 12,
              cursor: 'pointer'
            }}
          >
            JSON
          </button>

          <button
            onClick={handleDownloadPdf}
            style={{
              padding: '6px 12px',
              background: 'var(--accent-teal)',
              color: 'white',
              border: 'none',
              borderRadius: 6,
              fontSize: 12,
              cursor: 'pointer'
            }}
          >
            PDF
          </button>
        </div>
      </div>

      <div style={{
        background: 'var(--navy)',
        borderRadius: 'var(--radius)',
        marginBottom: 16,
        display: 'flex',
        overflowX: 'auto'
      }}>
        {tabs.map((tab, idx) => (
          <button
            key={idx}
            onClick={() => setActiveTab(idx)}
            style={{
              padding: '12px 16px',
              background: activeTab === idx ? 'rgba(201, 148, 58, 0.15)' : 'transparent',
              color: activeTab === idx ? 'var(--gold)' : 'var(--text-muted)',
              border: 'none',
              borderBottom: activeTab === idx ? '3px solid var(--gold)' : '3px solid transparent',
              fontSize: 13,
              fontWeight: activeTab === idx ? 600 : 400,
              cursor: 'pointer',
              whiteSpace: 'nowrap'
            }}
          >
            {tab}
          </button>
        ))}
      </div>

      <div style={{
        background: 'white',
        padding: 24,
        borderRadius: 'var(--radius)',
        border: '1px solid var(--border)'
      }}>
        {activeTab === 0 && <PersonalInfoTab formData={formData} setFormData={setFormData} />}
        {activeTab === 1 && <SalaryTab formData={formData} setFormData={setFormData} taxResult={taxResult} />}
        {activeTab === 2 && <HousePropertyTab formData={formData} setFormData={setFormData} taxResult={taxResult} />}
        {activeTab === 3 && <CapitalGainsTab formData={formData} setFormData={setFormData} taxResult={taxResult} year={year!} />}
        {activeTab === 4 && <BusinessTab formData={formData} setFormData={setFormData} taxResult={taxResult} />}
        {activeTab === 5 && <OtherSourcesTab formData={formData} setFormData={setFormData} taxResult={taxResult} />}
        {activeTab === 6 && <VDATab formData={formData} setFormData={setFormData} taxResult={taxResult} />}
        {activeTab === 7 && <DeductionsTab formData={formData} setFormData={setFormData} regime={regime} taxResult={taxResult} />}
        {activeTab === 8 && <LossesTab formData={formData} setFormData={setFormData} />}
        {activeTab === 9 && <TDSTab formData={formData} setFormData={setFormData} taxResult={taxResult} />}
        {activeTab === 10 && <TaxComputationTab taxResult={taxResult} regime={regime} />}
      </div>
    </div>
  );
}

function Field({ label, value, onChange, computed, prefix = '₹', type = 'number' }: any) {
  return (
    <div style={{ marginBottom: 16 }}>
      <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: 'var(--text-secondary)' }}>
        {label}
      </label>
      <div style={{ position: 'relative' }}>
        {prefix && !computed && (
          <span style={{
            position: 'absolute',
            left: 12,
            top: '50%',
            transform: 'translateY(-50%)',
            color: 'var(--text-muted)',
            fontSize: 13
          }}>
            {prefix}
          </span>
        )}
        <input
          type={type}
          value={value}
          onChange={(e) => !computed && onChange(type === 'number' ? Number(e.target.value) : e.target.value)}
          readOnly={computed}
          style={{
            width: '100%',
            padding: '8px 12px',
            paddingLeft: prefix && !computed ? 28 : 12,
            border: '1px solid var(--border)',
            borderRadius: 6,
            fontSize: 13,
            background: computed ? 'var(--gold-pale)' : 'white',
            cursor: computed ? 'default' : 'text',
            fontFamily: type === 'number' ? 'DM Mono' : 'inherit'
          }}
        />
      </div>
    </div>
  );
}

function PersonalInfoTab({ formData, setFormData }: any) {
  const calculateAge = (dob: string) => {
    if (!dob) return 0;
    const birthDate = new Date(dob);
    const refDate = new Date('2026-03-31'); // Age as on 31st March of AY
    let age = refDate.getFullYear() - birthDate.getFullYear();
    const monthDiff = refDate.getMonth() - birthDate.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && refDate.getDate() < birthDate.getDate())) {
      age--;
    }
    return age;
  };

  const handleDobChange = (dob: string) => {
    const age = calculateAge(dob);
    setFormData({ ...formData, dob, age });
  };

  return (
    <div>
      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Part A - General Information (Auto-populated from Client Master)
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="Name of Assessee" value={formData.name || ''} onChange={(v: any) => setFormData({ ...formData, name: v })} type="text" prefix="" />
        <Field label="PAN" value={formData.pan || ''} onChange={(v: any) => setFormData({ ...formData, pan: v })} type="text" prefix="" />
        <Field label="Aadhaar Number" value={formData.aadhaar || ''} onChange={(v: any) => setFormData({ ...formData, aadhaar: v })} type="text" prefix="" />
        <Field label="Date of Birth / Formation" value={formData.dob || ''} onChange={handleDobChange} type="date" prefix="" />
        <Field label="Age as on 31/03" value={formData.age} computed prefix="" />
        <Field label="Status" value={formData.status || 'Individual'} onChange={(v: any) => setFormData({ ...formData, status: v })} type="text" prefix="" />
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Contact Details
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="Email Address" value={formData.email || ''} onChange={(v: any) => setFormData({ ...formData, email: v })} type="email" prefix="" />
        <Field label="Mobile Number" value={formData.mobile || ''} onChange={(v: any) => setFormData({ ...formData, mobile: v })} type="tel" prefix="" />
        <Field label="Telephone (STD-Number)" value={formData.telephone || ''} onChange={(v: any) => setFormData({ ...formData, telephone: v })} type="tel" prefix="" />
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Address for Communication
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="Flat/Door/Block No." value={formData.flatNo || ''} onChange={(v: any) => setFormData({ ...formData, flatNo: v })} type="text" prefix="" />
        <Field label="Name of Premises/Building/Village" value={formData.premises || ''} onChange={(v: any) => setFormData({ ...formData, premises: v })} type="text" prefix="" />
        <Field label="Road/Street/Post Office" value={formData.road || ''} onChange={(v: any) => setFormData({ ...formData, road: v })} type="text" prefix="" />
        <Field label="Area/Locality" value={formData.area || ''} onChange={(v: any) => setFormData({ ...formData, area: v })} type="text" prefix="" />
        <Field label="Town/City/District" value={formData.city || ''} onChange={(v: any) => setFormData({ ...formData, city: v })} type="text" prefix="" />
        <Field label="State" value={formData.state || ''} onChange={(v: any) => setFormData({ ...formData, state: v })} type="text" prefix="" />
        <Field label="PIN Code" value={formData.pincode || ''} onChange={(v: any) => setFormData({ ...formData, pincode: v })} type="text" prefix="" />
        <Field label="Country" value={formData.country || 'India'} onChange={(v: any) => setFormData({ ...formData, country: v })} type="text" prefix="" />
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Filing Details
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        <div>
          <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: 'var(--text-secondary)' }}>
            Return Filed u/s
          </label>
          <select
            value={formData.filingSection || '139(1)'}
            onChange={(e) => setFormData({ ...formData, filingSection: e.target.value })}
            style={{
              width: '100%',
              padding: '8px 12px',
              border: '1px solid var(--border)',
              borderRadius: 6,
              fontSize: 13
            }}
          >
            <option value="139(1)">139(1) - On or before due date</option>
            <option value="139(4)">139(4) - Belated return</option>
            <option value="139(5)">139(5) - Revised return</option>
            <option value="119(2)(b)">119(2)(b) - After condonation of delay</option>
          </select>
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: 'var(--text-secondary)' }}>
            Residential Status
          </label>
          <select
            value={formData.residentialStatus || 'ROR'}
            onChange={(e) => setFormData({ ...formData, residentialStatus: e.target.value })}
            style={{
              width: '100%',
              padding: '8px 12px',
              border: '1px solid var(--border)',
              borderRadius: 6,
              fontSize: 13
            }}
          >
            <option value="ROR">Resident</option>
            <option value="RNOR">Resident but Not Ordinarily Resident</option>
            <option value="NR">Non-Resident</option>
          </select>
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: 'var(--text-secondary)' }}>
            Employer Category
          </label>
          <select
            value={formData.employerCategory || 'OTH'}
            onChange={(e) => setFormData({ ...formData, employerCategory: e.target.value })}
            style={{
              width: '100%',
              padding: '8px 12px',
              border: '1px solid var(--border)',
              borderRadius: 6,
              fontSize: 13
            }}
          >
            <option value="GOV">Government</option>
            <option value="PSU">PSU</option>
            <option value="PE">Pensioners</option>
            <option value="OTH">Others</option>
          </select>
        </div>
      </div>

      <div style={{ marginTop: 16, padding: 12, background: 'var(--info-bg)', borderRadius: 6, fontSize: 12, color: 'var(--info)' }}>
        ℹ️ Personal information is auto-populated from Client Master and imported documents (26AS, AIS, TIS). PAN validation ensures data integrity.
      </div>
    </div>
  );
}

function SalaryTab({ formData, setFormData, taxResult }: any) {
  return (
    <div>
      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Employer Details (CBDT Mandatory)
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="Employer Name *" value={formData.employerName || ''} onChange={(v: any) => setFormData({ ...formData, employerName: v })} type="text" prefix="" />
        <Field label="Employer TAN *" value={formData.employerTAN || ''} onChange={(v: any) => setFormData({ ...formData, employerTAN: v })} type="text" prefix="" />
        <Field label="Employer Address *" value={formData.employerAddress || ''} onChange={(v: any) => setFormData({ ...formData, employerAddress: v })} type="text" prefix="" />
        <div>
          <label style={{ display: 'block', marginBottom: 6, fontSize: 12, fontWeight: 500, color: 'var(--text-secondary)' }}>
            Nature of Employment *
          </label>
          <select
            value={formData.natureOfEmployment || 'OTH'}
            onChange={(e) => setFormData({ ...formData, natureOfEmployment: e.target.value })}
            style={{
              width: '100%',
              padding: '8px 12px',
              border: '1px solid var(--border)',
              borderRadius: 6,
              fontSize: 13
            }}
          >
            <option value="GOV">Government</option>
            <option value="PSU">PSU</option>
            <option value="PE">Pensioners</option>
            <option value="OTH">Others</option>
          </select>
        </div>
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Salary Components
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        <Field label="Basic Salary" value={formData.basic} onChange={(v: any) => setFormData({ ...formData, basic: v })} />
        <Field label="DA" value={formData.da} onChange={(v: any) => setFormData({ ...formData, da: v })} />
        <Field label="HRA Received" value={formData.hra} onChange={(v: any) => setFormData({ ...formData, hra: v })} />
        <Field label="Bonus/Incentives" value={formData.bonus} onChange={(v: any) => setFormData({ ...formData, bonus: v })} />
        <Field label="Special Allowances" value={formData.allowances} onChange={(v: any) => setFormData({ ...formData, allowances: v })} />
        <Field label="Perquisites" value={formData.perquisites} onChange={(v: any) => setFormData({ ...formData, perquisites: v })} />
        <Field label="Gross Salary" value={taxResult.grossSalary} computed />
        <Field label="HRA Rent Paid p.a." value={formData.hraRent} onChange={(v: any) => setFormData({ ...formData, hraRent: v })} />
        <Field label="Professional Tax" value={formData.profTax} onChange={(v: any) => setFormData({ ...formData, profTax: v })} />
        <Field label="HRA Exempt" value={taxResult.hraExempt} computed />
        <Field label="Standard Deduction" value={75000} computed />
        <Field label="Net Taxable Salary" value={taxResult.netSalary} computed />
      </div>
      <div style={{ marginTop: 12 }}>
        <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13 }}>
          <input
            type="checkbox"
            checked={formData.hraMetro}
            onChange={(e) => setFormData({ ...formData, hraMetro: e.target.checked })}
          />
          Metro City (50% exemption)
        </label>
      </div>
    </div>
  );
}

function HousePropertyTab({ formData, setFormData, taxResult }: any) {
  return (
    <div>
      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Property Details (CBDT Mandatory)
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="Property Address *" value={formData.hpAddress || ''} onChange={(v: any) => setFormData({ ...formData, hpAddress: v })} type="text" prefix="" />
        <Field label="City *" value={formData.hpCity || ''} onChange={(v: any) => setFormData({ ...formData, hpCity: v })} type="text" prefix="" />
        <Field label="PIN Code *" value={formData.hpPincode || ''} onChange={(v: any) => setFormData({ ...formData, hpPincode: v })} type="text" prefix="" />
        <Field label="Ownership % *" value={formData.hpOwnershipPct || 100} onChange={(v: any) => setFormData({ ...formData, hpOwnershipPct: v })} prefix="" />
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Co-owner Details (if applicable)
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 24 }}>
        <Field label="Co-owner Name" value={formData.hpCoOwnerName || ''} onChange={(v: any) => setFormData({ ...formData, hpCoOwnerName: v })} type="text" prefix="" />
        <Field label="Co-owner PAN" value={formData.hpCoOwnerPAN || ''} onChange={(v: any) => setFormData({ ...formData, hpCoOwnerPAN: v })} type="text" prefix="" />
        <Field label="Co-owner Share %" value={formData.hpCoOwnerPct || 0} onChange={(v: any) => setFormData({ ...formData, hpCoOwnerPct: v })} prefix="" />
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Property Type & Income
      </h3>
      <div style={{ marginBottom: 16 }}>
        <label style={{ display: 'block', marginBottom: 8, fontSize: 12, fontWeight: 500 }}>Property Type</label>
        <div style={{ display: 'flex', gap: 12 }}>
          {['self', 'letout'].map(type => (
            <button
              key={type}
              onClick={() => setFormData({ ...formData, hpType: type })}
              style={{
                padding: '8px 16px',
                background: formData.hpType === type ? 'var(--gold)' : 'var(--bg)',
                color: formData.hpType === type ? 'white' : 'var(--text-primary)',
                border: '1px solid var(--border)',
                borderRadius: 6,
                fontSize: 13,
                cursor: 'pointer'
              }}
            >
              {type === 'self' ? 'Self Occupied' : 'Let Out'}
            </button>
          ))}
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        {formData.hpType === 'letout' ? (
          <>
            <Field label="Gross Annual Rent" value={formData.grossRent} onChange={(v: any) => setFormData({ ...formData, grossRent: v })} />
            <Field label="Municipal Tax Paid" value={formData.munTax} onChange={(v: any) => setFormData({ ...formData, munTax: v })} />
            <Field label="Home Loan Interest" value={formData.homeLoanInt} onChange={(v: any) => setFormData({ ...formData, homeLoanInt: v })} />
            <Field label="Tenant Name" value={formData.hpTenantName || ''} onChange={(v: any) => setFormData({ ...formData, hpTenantName: v })} type="text" prefix="" />
            <Field label="Tenant PAN" value={formData.hpTenantPAN || ''} onChange={(v: any) => setFormData({ ...formData, hpTenantPAN: v })} type="text" prefix="" />
          </>
        ) : (
          <Field label="SOP Loan Interest (max ₹2L)" value={formData.sopLoanInt} onChange={(v: any) => setFormData({ ...formData, sopLoanInt: v })} />
        )}
        <Field label="Net HP Income" value={taxResult.hpIncome} computed />
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginTop: 24, marginBottom: 16, color: 'var(--text-secondary)' }}>
        Lender Details (if loan exists)
      </h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        <Field label="Lender Name" value={formData.hpLenderName || ''} onChange={(v: any) => setFormData({ ...formData, hpLenderName: v })} type="text" prefix="" />
        <Field label="Lender PAN" value={formData.hpLenderPAN || ''} onChange={(v: any) => setFormData({ ...formData, hpLenderPAN: v })} type="text" prefix="" />
      </div>
    </div>
  );
}

function CapitalGainsTab({ formData, setFormData, taxResult }: any) {
  return (
    <div>
      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>Short Term Capital Gains</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 20 }}>
        <Field label="Pre 23 Jul 2024 (15%)" value={formData.stcgPre} onChange={(v: any) => setFormData({ ...formData, stcgPre: v })} />
        <Field label="Post 23 Jul 2024 (20%)" value={formData.stcgPost} onChange={(v: any) => setFormData({ ...formData, stcgPost: v })} />
        <Field label="Other Assets (20%)" value={formData.stcgOther} onChange={(v: any) => setFormData({ ...formData, stcgOther: v })} />
      </div>

      <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 12 }}>Long Term Capital Gains</h3>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        <Field label="Pre 23 Jul 2024" value={formData.ltcgPre} onChange={(v: any) => setFormData({ ...formData, ltcgPre: v })} />
        <Field label="Post 23 Jul 2024" value={formData.ltcgPost} onChange={(v: any) => setFormData({ ...formData, ltcgPost: v })} />
        <Field label="Other Assets" value={formData.ltcgOther} onChange={(v: any) => setFormData({ ...formData, ltcgOther: v })} />
        <Field label="Total CG Tax" value={taxResult.cgTax} computed />
      </div>
    </div>
  );
}
