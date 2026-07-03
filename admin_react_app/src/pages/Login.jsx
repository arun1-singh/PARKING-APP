import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import Input from '../components/forms/Input';
import Button from '../components/common/Button';
import { validateLoginForm } from '../utils/validators';
import { DEMO_CREDENTIALS } from '../utils/constants';
import { determineRoleFromEmail } from '../utils/helpers';

const Login = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated, isLoading } = useAuth();
  
  const [formData, setFormData] = useState({
    user_email: '',
    user_password: '',
  });
  
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loginError, setLoginError] = useState('');

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated) {
      const from = location.state?.from || '/dashboard';
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, navigate, location.state]);

  // Handle input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value,
    }));
    
    // Clear field error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: '',
      }));
    }
    
    // Clear login error
    if (loginError) {
      setLoginError('');
    }
  };

  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validate form
    const validation = validateLoginForm(formData);
    if (!validation.isValid) {
      setErrors(validation.errors);
      return;
    }
    
    setIsSubmitting(true);
    setLoginError('');
    
    try {
      // Auto-determine role based on email
      const role = determineRoleFromEmail(formData.user_email) || 'admin';
      
      // Create login payload with auto-determined role
      const loginPayload = {
        user_email: formData.user_email,
        user_password: formData.user_password,
        role: role,
      };
      
      await login(loginPayload);
    } catch (error) {
      setLoginError(error.message || 'Login failed. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle demo credential selection
  const handleDemoLogin = (demoType) => {
    const credentials = DEMO_CREDENTIALS[demoType];
    setFormData({
      user_email: credentials.user_email,
      user_password: credentials.user_password,
    });
    setErrors({});
    setLoginError('');
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-700 font-medium">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-gray-100 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        {/* Header */}
        <div className="text-center">
          <div className="mx-auto w-16 h-16 bg-blue-600 rounded-full flex items-center justify-center mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
          <h2 className="text-3xl font-bold text-gray-900">
            Admin Portal
          </h2>
          <p className="mt-2 text-gray-700">
            Sign in to your administrator account
          </p>
        </div>

        {/* Login Form */}
        <div className="bg-white py-8 px-6 shadow-xl rounded-xl border border-gray-100">
          <form className="space-y-6" onSubmit={handleSubmit}>
            {/* Email Input */}
            <div>
              <label htmlFor="user_email" className="block text-sm font-medium text-gray-700 mb-1">
                Email Address
              </label>
              <Input
                name="user_email"
                type="email"
                value={formData.user_email}
                onChange={handleInputChange}
                placeholder="Enter your email"
                required
                error={errors.user_email}
                autoComplete="email"
                className="text-gray-900 placeholder-gray-500"
              />
            </div>

            {/* Password Input */}
            <div>
              <label htmlFor="user_password" className="block text-sm font-medium text-gray-700 mb-1">
                Password
              </label>
              <Input
                name="user_password"
                type="password"
                value={formData.user_password}
                onChange={handleInputChange}
                placeholder="Enter your password"
                required
                error={errors.user_password}
                autoComplete="current-password"
                className="text-gray-900 placeholder-gray-500"
              />
            </div>

            {/* Login Error */}
            {loginError && (
              <div className="bg-red-50 border border-red-200 rounded-md p-4">
                <div className="flex">
                  <div className="flex-shrink-0">
                    <svg className="h-5 w-5 text-red-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                      <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                    </svg>
                  </div>
                  <div className="ml-3">
                    <h3 className="text-sm font-medium text-red-800">Login Error</h3>
                    <p className="text-sm text-red-700 mt-1">{loginError}</p>
                  </div>
                </div>
              </div>
            )}

            {/* Submit Button */}
            <Button
              type="submit"
              variant="primary"
              size="lg"
              className="w-full flex justify-center py-3 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              loading={isSubmitting}
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Signing in...' : 'Sign in'}
            </Button>
          </form>

          {/* Demo Credentials Section */}
          <div className="mt-8 pt-6 border-t border-gray-200">
            <h3 className="text-sm font-medium text-gray-900 mb-4 text-center">
              Demo Credentials
            </h3>
            
            <div className="grid gap-3">
              {/* Super Admin Demo */}
              <div className="bg-blue-50 p-3 rounded-lg">
                <p className="text-xs font-semibold text-blue-800 mb-1">Super Admin</p>
                <button
                  type="button"
                  onClick={() => handleDemoLogin('SUPER_ADMIN')}
                  disabled={isSubmitting}
                  className="text-sm text-blue-700 hover:text-blue-900 underline disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200 w-full text-left"
                >
                  <span className="block truncate">{DEMO_CREDENTIALS.SUPER_ADMIN.user_email}</span>
                  <span className="text-blue-600 font-mono">Click to autofill credentials</span>
                </button>
              </div>

              {/* Admin Demo */}
              <div className="bg-green-50 p-3 rounded-lg">
                <p className="text-xs font-semibold text-green-800 mb-1">Admin</p>
                <button
                  type="button"
                  onClick={() => handleDemoLogin('ADMIN')}
                  disabled={isSubmitting}
                  className="text-sm text-green-700 hover:text-green-900 underline disabled:opacity-50 disabled:cursor-not-allowed transition-colors duration-200 w-full text-left"
                >
                  <span className="block truncate">{DEMO_CREDENTIALS.ADMIN.user_email}</span>
                  <span className="text-green-600 font-mono">Click to autofill credentials</span>
                </button>
              </div>
            </div>
            
            <p className="text-xs text-gray-500 mt-4 text-center">
              Select a demo account to automatically fill the login form
            </p>
            <p className="text-xs text-gray-500 mt-4 text-center">
              Every Admin Has the same password : admin123
            </p>
            
          </div>
        </div>
        
        {/* Footer */}
        <div className="text-center">
          <p className="text-xs text-gray-500">
            © 2025 Parking Admin Dashboard. All rights reserved.
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;